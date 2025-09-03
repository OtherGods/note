在我们的项目中，有用户的邀请功能，每一次邀请别人注册，会有一定的积分，然后我们同时提供了一个排行榜的功能，可以基于这个积分进行排名。

排名的功能比较简单，就是基于积分去排序就行了，这里面我们利用了Redis的ZSET的数据结构实现快速的排序。

因为ZSET是一个天然有序的数据结构，我们可以把积分当做score，用户id当做member，放到zset中，zset会默认按照SCORE进行排序的。

以下是用户接受邀请部分的代码实现：
```java
@DistributeLock(keyExpression = "#telephone", scene = "USER_REGISTER")  
public UserOperatorResponse register(String telephone, String inviteCode) {
    //用户名生成  
    String inviterId = null;  
    if (StringUtils.isNotBlank(inviteCode)) {  
        User inviter = userMapper.findByInviteCode(inviteCode);  
        if (inviter != null) {  
            inviterId = inviter.getId().toString();  
        }  
    }  
      
    //用户注册  
    //更新排名  
    updateInviteRank(inviterId);  
      
      
    //其他逻辑  
}
```

updateInviteRank的额代码逻辑如下：
```java
private void updateInviteRank(String inviterId) {  
    // 如果邀请者ID为空，则直接返回，不进行操作  
    if (inviterId == null) {  
        return;  
    }  
      
    // 获取Redisson的锁对象  
    RLock rLock = redissonClient.getLock(inviterId);  
    // 对邀请者ID对应的锁进行加锁操作，避免并发更新  
    rLock.lock();  
    try {  
        // 获取邀请者的当前排名分数  
        Double score = inviteRank.getScore(inviterId);  
        // 如果当前分数为空，则设置默认为0.0  
        if (score == null) {  
            score = 0.0;  
        }  
        // 将邀请者的排名分数增加100.0，并更新到排行榜中  
        inviteRank.add(score + 100.0, inviterId);  
    } finally {  
        // 最终释放邀请者ID对应的锁  
        rLock.unlock();  
    }  
}
```

这里主要是用到了Redisson的RLock进行了加锁，并且是用的lock方法，在加锁失败时阻塞一直尝试。主要就是避免多个用户同时被邀请时，更新分数会出现并发而导致分数累加错误。

这里面的排行榜inviteRank，其实是:
```java
private RScoredSortedSet<String> inviteRank;  
  
@Override  
	public void afterPropertiesSet() throws Exception {  
      
    this.inviteRank = redissonClient.getScoredSortedSet("inviteRank");  
}
```

在以上逻辑中进行初始化和实例化的，其实他是一个RScoredSortedSet，是一个支持排序的Set，他提供了很多方法可以方便的实现排名的功能，如：
- **getScore**：获取指定成员的分数。
- **add**：向有序集合中添加一个成员，指定该成员的分数。
- **rank**：获取指定成员在有序集合中的排名（从小到大排序，排名从 0 开始）。
- **revRank**：获取指定成员在有序集合中的排名（从大到小排序，排名从 0 开始）。
- **entryRange**：获取分数在指定范围内的成员及其分数的集合。

比如我们提供了以下几个和排名有关的方法，其实就是对上述方法的一些封装：
```java
//获取指定用户的排名，按照分数从高到低  
public Integer getInviteRank(String userId) {  
    Integer rank = inviteRank.revRank(userId);  
    if (rank != null) {  
        return rank + 1;  
    }  
    return null;  
}
```

```java
//按照分数从高到低，获取前N个用户的排名信息  
public List<InviteRankInfo> getTopN(Integer topN) {
    Collection<ScoredEntry<String>> rankInfos = inviteRank.entryRangeReversed(0, topN - 1);  
      
    List<InviteRankInfo> inviteRankInfos = new ArrayList<>();  
      
    if (rankInfos != null) {  
        for (ScoredEntry<String> rankInfo : rankInfos) {  
            InviteRankInfo inviteRankInfo = new InviteRankInfo();  
            String userId = rankInfo.getValue();  
            if (StringUtils.isNotBlank(userId)) {  
                User user = findById(Long.valueOf(userId));  
                if (user != null) {  
                    inviteRankInfo.setNickName(user.getNickName());  
                    inviteRankInfo.setInviteCode(user.getInviteCode());  
                    inviteRankInfo.setInviteCount(rankInfo.getScore().intValue() / 100);  
                    inviteRankInfos.add(inviteRankInfo);  
                }  
            }  
        }  
    }  
      
    return inviteRankInfos;  
}
```

### 分数相同时，按照上榜时间排序

[2、基于 ZSET 的多维度排行榜实现](2、相关技术/24、项目/3、数藏项目/8、最佳实践/9、排行榜/2、基于%20ZSET%20的多维度排行榜实现.md)

