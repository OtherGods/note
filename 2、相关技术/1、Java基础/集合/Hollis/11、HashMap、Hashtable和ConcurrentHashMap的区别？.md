# 典型回答

## 线程安全

1. HashMap是非线程安全的
2. Hashtable 中的方法是同步的，所以它是线程安全的
3. ConcurrentHashMap在JDK 1.8之前使用分段锁保证线程安全， ConcurrentHashMap默认情况下将hash表分为16个桶（分片），在加锁的时候，针对每个单独的分片进行加锁，其他分片不受影响。锁的粒度更细，所以他的性能更好。

ConcurrentHashMap在JDK 1.8中，采用了一种新的方式来实现线程安全，即使用了CAS+synchronized，这个实现被称为"分段锁"的变种，也被称为"锁分离"，它将锁定粒度更细，把锁的粒度从整个Map降低到了单个桶。

## 继承关系

1. Hashtable是基于陈旧的Dictionary类继承来的
2. HashMap继承的抽象类AbstractMap实现了Map接口
3. ConcurrentHashMap同样继承了抽象类AbstractMap，并且实现了ConcurrentMap接口。

## 允不允许null值

1. HashTable中，key和value都不允许出现null值，否则会抛出NullPointerException异常。 
2. HashMap中，null可以作为键或者值都可以。
3. ConcurrentHashMap中，key和value都不允许为null。

[29、为什么ConcurrentHashMap不允许null值？](2、相关技术/1、Java基础/集合/Hollis/29、为什么ConcurrentHashMap不允许null值？.md)

## 默认初始容量和扩容机制

1. HashMap的默认初始容量为16，默认的加载因子为0.75，即当HashMap中元素个数超过容量的75%时，会进行扩容操作。扩容时，**容量会扩大为原来的两倍**，并将原来的元素重新分配到新的桶中。
2. Hashtable，默认初始容量为11，默认的加载因子为0.75，即当Hashtable中元素个数超过容量的75%时，会进行扩容操作。扩容时，**容量会扩大为原来的两倍加1**，并将原来的元素重新分配到新的桶中。
3. ConcurrentHashMap，默认初始容量为16，默认的加载因子为0.75，即当ConcurrentHashMap中元素个数超过容量的75%时，会进行扩容操作。扩容时，**容量会扩大为原来的两倍**，并会**采用分段锁机制，将ConcurrentHashMap分为多个段(segment)，每个段独立进行扩容操作，避免了整个ConcurrentHashMap的锁竞争**。
 

## 遍历方式的内部实现上不同

1. HashMap使用**entrySet()** 进行遍历，即先获取到HashMap中所有的键值对(Entry)，然后遍历Entry集合。支持fail-fast，也就是说在遍历过程中，若HashMap的结构被修改（添加或删除元素），则会抛出ConcurrentModificationException。如果只需要遍历HashMap中的key或value，可以使用**KeySet**或**Values**来遍历。
2. Hashtable使用**Enumeration**进行遍历，即获取Hashtable中所有的key，然后遍历key集合。这个过程也会判断是否存在并发修改：
```java
public T next() {
    if (modCount != expectedModCount)
        throw new ConcurrentModificationException();
    return nextElement();
}

public void remove() {
    if (!iterator)
        throw new UnsupportedOperationException();
    if (lastReturned == null)
        throw new IllegalStateException("Hashtable Enumerator");
    if (modCount != expectedModCount)
        throw new ConcurrentModificationException();

    synchronized(Hashtable.this) {
        Entry[] tab = Hashtable.this.table;
        int index = (lastReturned.hash & 0x7FFFFFFF) % tab.length;

        for (Entry<K,V> e = tab[index], prev = null; e != null;
             prev = e, e = e.next) {
            if (e == lastReturned) {
                modCount++;
                expectedModCount++;
                if (prev == null)
                    tab[index] = e.next;
                else
                    prev.next = e.next;
                count--;
                lastReturned = null;
                return;
            }
        }
        throw new ConcurrentModificationException();
    }
}
```

3. ConcurrentHashMap**使用分段锁机制**，因此在遍历时需要注意，**遍历时ConcurrentHashMap的某个段被修改不会影响其他段的遍历**。可以使用**EntrySet**、**KeySet**或**Values**来遍历ConcurrentHashMap，其中EntrySet遍历时效率最高。*遍历过程中，ConcurrentHashMap的结构发生变化时，不会抛出ConcurrentModificationException异常，但是在遍历时可能会出现数据不一致的情况，因为遍历器仅提供了弱一致性保障*。

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202507082322478.png)

