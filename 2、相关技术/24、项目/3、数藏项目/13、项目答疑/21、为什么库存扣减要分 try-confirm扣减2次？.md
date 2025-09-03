数字藏品，都是数量比较有限的，比如说发售100份，那么每一个购买者，就都会有一个唯一的编号，001、002、003 这样的编号。而这个编号，需要根据一定的顺序生成，比如支付时间或者下单时间。这样就能保证早支付（下单）的用户的编号更靠前。

而在数藏业务中，这个编号其实还挺重要的，很多收藏着比较在意自己的编号是不是更靠前，是不是更顺等等。

所以，我们在生成编号这里需要考虑如何生成。

最简单的生成编号的方式就是用`总量-库存+1` ，假设总量200份，当前库存还剩200份，那么来购买的人的编号应该就是1；那如果总量是200分，当前库存还剩20分，那么这时候购买的人的编号应该是181。如果当前库存还剩最后一个，则购买者的编号应该是`200 - 1 + 1 = 200` 。

所以，我们只需要在藏品上记录上总量和剩余库存即可计算出编号了。

那么，确定了编号生成的方式之后，该在什么时候生成这个编号呢？下单的时候，还是支付的时候？

下单的时候实现比较简单，在 redis中预扣减的时候就能拿到库存数了，这时候就可以确定下来一个唯一的编号。但是这个方案不好，最重要的就是，万一用户下单后未支付，这个编号就会被空空出来。后面如果订单关单了，用户重新下单的时候，新编号就很难知道哪个编号没用过，而且更重要的是即使知道了是哪个编号，这样干也会出现后下单的编号更靠前的问题。

所以，我们在支付成功后再生成这个编号比较合理的，因为这时候已经付款了，held_collection 也要正式生成了，这时候获取一个编号最合理了。

那么，这就带来一个新的问题。藏品的库存下单的时候就扣减了，支付的时候看到的库存已经不准了，根本不知道这一刻用户是第几个支付成功的。

为了解决这个问题，我们定义了两个库存，一个在下单的时候扣减，一个在支付的时候扣减。

下单库存我们定义为 saleableInventory，表示当前可售库存有多少，在 trySale 方法中进行扣减，这个库存用来避免出现超卖，以及减少支付成功后无库存可扣减的问题。

```sql
<update id="trySale">
    UPDATE collection
    SET saleable_inventory = saleable_inventory - #{quantity}, lock_version = lock_version + 1,gmt_modified = now()
    WHERE id = #{id} and <![CDATA[saleable_inventory >= #{quantity}]]>
</update>
```

支付库存我们定义为occupiedInventory，表示当前已占用库存是多少，在confimSale 方法中进行累加。这个库存用来记录当前已经有多少订单已支付，用来生成藏品编号。

```sql
<update id="confirmSale">
    UPDATE  collection
    SET occupied_inventory = occupied_inventory + #{quantity}, lock_version = lock_version + 1,gmt_modified = now()
    WHERE id = #{id} and <![CDATA[occupied_inventory + #{quantity} <= quantity ]]>
</update>
```



