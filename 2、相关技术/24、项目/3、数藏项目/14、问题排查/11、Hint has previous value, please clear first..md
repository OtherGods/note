```java
java.lang.IllegalStateException: java.lang.IllegalStateException: Hint has previous value, please clear first.  
at java.base/jdk.internal.reflect.DirectConstructorHandleAccessor.newInstance(DirectConstructorHandleAccessor.java:62)  
at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:502)  
at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:486)  
at java.base/java.util.concurrent.ForkJoinTask.getThrowableException(ForkJoinTask.java:540)  
at java.base/java.util.concurrent.ForkJoinTask.reportException(ForkJoinTask.java:567)  
at java.base/java.util.concurrent.ForkJoinTask.invoke(ForkJoinTask.java:670)  
at java.base/java.util.stream.ForEachOps$ForEachOp.evaluateParallel(ForEachOps.java:160)  
at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.evaluateParallel(ForEachOps.java:174)  
at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:233)  
at java.base/java.util.stream.ReferencePipeline.forEach(ReferencePipeline.java:596)  
at java.base/java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:765)  
at cn.hollis.nft.turbo.order.job.OrderJob.orderTimeOutExecute(OrderJob.java:81)  
at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)  
at java.base/java.lang.reflect.Method.invoke(Method.java:580)  
at com.xxl.job.core.handler.impl.MethodJobHandler.execute(MethodJobHandler.java:31)  
at com.xxl.job.core.thread.JobThread$1.call(JobThread.java:146)  
at com.xxl.job.core.thread.JobThread$1.call(JobThread.java:139)  
at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:317)  
at java.base/java.lang.Thread.run(Thread.java:1583)  
Caused by: java.lang.IllegalStateException: Hint has previous value, please clear first.  
at com.google.common.base.Preconditions.checkState(Preconditions.java:512)  
at org.apache.shardingsphere.infra.hint.HintManager.getInstance(HintManager.java:51)  
at cn.hollis.nft.turbo.order.job.OrderJob.lambda$orderTimeOutExecute$0(OrderJob.java:83)  
at java.base/java.util.stream.ForEachOps$ForEachOp$OfRef.accept(ForEachOps.java:184)  
at java.base/java.util.ArrayList$ArrayListSpliterator.forEachRemaining(ArrayList.java:1708)  
at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:509)  
at java.base/java.util.stream.ForEachOps$ForEachTask.compute(ForEachOps.java:291)  
at java.base/java.util.concurrent.CountedCompleter.exec(CountedCompleter.java:754)  
at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:387)  
at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1312)  
at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1843)  
at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1808)  
at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:188)
```

这个异常发生在订单超时任务执行的时候，原来的代码如下：
```java
shardingTableIndexes.parallelStream().forEach(index -> {  
    HintManager hintManager = HintManager.getInstance();  
    hintManager.addTableShardingValue("trade_order", "000" + index);  
    int currentPage = 1;  
    Page<TradeOrder> page = orderReadService.pageQueryTimeoutOrders(currentPage, PAGE_SIZE);  
    page.getRecords().forEach(this::executeTimeoutSingle);  
    while (page.hasNext()) {  
        currentPage++;  
        page = orderReadService.pageQueryTimeoutOrders(currentPage, PAGE_SIZE);  
        page.getRecords().forEach(this::executeTimeoutSingle);  
    }  
});
```

在并行流中多次使用HintManager.getInstance();来获取一个 hintManager，但是因为HintManager的原理是将分片键值保存在ThreadLocal中，所以需要在操作结束时调用hintManager.close()来清除ThreadLocal中的内容。

否则就会报错。所以代码修改为：
```java
shardingTableIndexes.forEach(index -> {  
    try (HintManager hintManager = HintManager.getInstance()) {  
        LOG.info("shardIndex {} is execute", index);  
        hintManager.addTableShardingValue("trade_order", "000" + index);  
        int currentPage = 1;  
        Page<TradeOrder> page = orderReadService.pageQueryTimeoutOrders(currentPage, PAGE_SIZE);  
        page.getRecords().forEach(this::executeTimeoutSingle);  
        while (page.hasNext()) {  
            currentPage++;  
            page = orderReadService.pageQueryTimeoutOrders(currentPage, PAGE_SIZE);  
            page.getRecords().forEach(this::executeTimeoutSingle);  
        }  
    }  
});
```
