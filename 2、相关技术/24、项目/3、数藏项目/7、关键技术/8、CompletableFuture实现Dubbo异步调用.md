在我们的藏品的 confirmSale 中，我们需要查询用户信息，这地方因为有些操作不依赖用户服务，所以在调用userFacadeService的时候，用了CompletableFuture，实现了一一个异步请求。

（以下代码已经在项目中移除，调整掉了，原因不是因为不能用，而是我改成了分布式事务，把上链这块操作单独独立出来了，不需要提前查用户了，对性能增益不大。但是这个文档还是保留，用法值得参考）
```java
@Override  
@Facade  
public CollectionSaleResponse confirmSale(CollectionSaleRequest request) {  
    UserQueryRequest userQueryRequest = new UserQueryRequest();  
    userQueryRequest.setUserId(Long.valueOf(request.getUserId()));  
    CompletableFuture<UserQueryResponse<UserInfo>> queryUserFuture = CompletableFuture.supplyAsync(() -> userFacadeService.query(userQueryRequest));  
      
    CollectionConfirmSaleResponse collectionSaleResponse = collectionService.confirmSale(request);  
    CollectionSaleResponse response = new CollectionSaleResponse();  
      
    if (collectionSaleResponse.getSuccess()) {  
        Collection collection = collectionSaleResponse.getCollection();  
        HeldCollection heldCollection = collectionSaleResponse.getHeldCollection();  
          
        Thread.ofVirtual().start(() -> {  
            ChainProcessRequest chainProcessRequest = new ChainProcessRequest();  
            chainProcessRequest.setRecipient(getUserInfo(queryUserFuture).getBlockChainUrl());  
            chainProcessRequest.setClassId(collection.getClassId());  
            chainProcessRequest.setClassName(collection.getName());  
            chainProcessRequest.setSerialNo(heldCollection.getSerialNo());  
            chainFacadeService.mint(chainProcessRequest);  
        });  
          
        response.setSuccess(true);  
        response.setHeldCollectionId(heldCollection.getId());  
    } else {  
        response.setSuccess(false);  
        response.setResponseCode(collectionSaleResponse.getResponseCode());  
        response.setResponseMessage(collectionSaleResponse.getResponseMessage());  
    }  
      
    return response;  
}
```

然后在后面真正需要的地方再取出想要的值就行了：
```java
private UserInfo getUserInfo(CompletableFuture<UserQueryResponse<UserInfo>> queryUserFuture) {  
    UserQueryResponse<UserInfo> userQueryResponse;  
      
    try {  
        userQueryResponse = queryUserFuture.get();  
        if (!userQueryResponse.getSuccess() || null == userQueryResponse.getData()) {  
            throw new CollectionException(COLLECTION_USER_QUERY_FAIL);  
        }  
        return userQueryResponse.getData();  
    } catch (InterruptedException | ExecutionException e) {  
        throw new RuntimeException(e);  
    }  
}
```

对应代码（Git History中初始内容）：
```java
package cn.hollis.nft.turbo.collection.facade;  
  
import cn.hollis.nft.turbo.api.chain.constant.ChainOperateBizTypeEnum;  
import cn.hollis.nft.turbo.api.chain.constant.ChainOperateTypeEnum;  
import cn.hollis.nft.turbo.api.chain.request.ChainProcessRequest;  
import cn.hollis.nft.turbo.api.chain.service.ChainFacadeService;  
import cn.hollis.nft.turbo.api.collection.constant.HeldCollectionState;  
import cn.hollis.nft.turbo.api.collection.model.CollectionInventoryVO;  
import cn.hollis.nft.turbo.api.collection.model.CollectionVO;  
import cn.hollis.nft.turbo.api.collection.model.HeldCollectionVO;  
import cn.hollis.nft.turbo.api.collection.request.*;  
import cn.hollis.nft.turbo.api.collection.response.CollectionChainResponse;  
import cn.hollis.nft.turbo.api.collection.response.CollectionDestroyResponse;  
import cn.hollis.nft.turbo.api.collection.response.CollectionSaleResponse;  
import cn.hollis.nft.turbo.api.collection.response.CollectionTransferResponse;  
import cn.hollis.nft.turbo.api.collection.service.CollectionFacadeService;  
import cn.hollis.nft.turbo.api.user.request.UserQueryRequest;  
import cn.hollis.nft.turbo.api.user.response.UserQueryResponse;  
import cn.hollis.nft.turbo.api.user.response.data.UserInfo;  
import cn.hollis.nft.turbo.api.user.service.UserFacadeService;  
import cn.hollis.nft.turbo.base.response.PageResponse;  
import cn.hollis.nft.turbo.base.response.SingleResponse;  
import cn.hollis.nft.turbo.collection.domain.entity.Collection;  
import cn.hollis.nft.turbo.collection.domain.entity.HeldCollection;  
import cn.hollis.nft.turbo.collection.domain.entity.convertor.CollectionConvertor;  
import cn.hollis.nft.turbo.collection.domain.entity.convertor.HeldCollectionConvertor;  
import cn.hollis.nft.turbo.collection.domain.request.CollectionInventoryRequest;  
import cn.hollis.nft.turbo.collection.domain.request.HeldCollectionDestroyRequest;  
import cn.hollis.nft.turbo.collection.domain.request.HeldCollectionTransferRequest;  
import cn.hollis.nft.turbo.collection.domain.response.CollectionConfirmSaleResponse;  
import cn.hollis.nft.turbo.collection.domain.response.CollectionInventoryResponse;  
import cn.hollis.nft.turbo.collection.domain.service.CollectionService;  
import cn.hollis.nft.turbo.collection.domain.service.HeldCollectionService;  
import cn.hollis.nft.turbo.collection.domain.service.impl.CollectionInventoryRedisService;  
import cn.hollis.nft.turbo.collection.exception.CollectionException;  
import cn.hollis.nft.turbo.rpc.facade.Facade;  
import com.alibaba.fastjson.JSON;  
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;  
import org.apache.commons.lang3.StringUtils;  
import org.apache.dubbo.config.annotation.DubboService;  
import org.slf4j.Logger;  
import org.slf4j.LoggerFactory;  
import org.springframework.beans.BeanUtils;  
import org.springframework.beans.factory.annotation.Autowired;  
  
import java.util.concurrent.CompletableFuture;  
import java.util.concurrent.ExecutionException;  
  
import static cn.hollis.nft.turbo.collection.exception.CollectionErrorCode.*;  
  
/**  
 * 藏品服务  
 *  
 * @author hollis */@DubboService(version = "1.0.0")  
public class CollectionFacadeServiceImpl implements CollectionFacadeService {  
  
    private static final Logger logger = LoggerFactory.getLogger(CollectionFacadeServiceImpl.class);  
  
    @Autowired  
    private ChainFacadeService chainFacadeService;  
  
    @Autowired  
    private UserFacadeService userFacadeService;  
  
    @Autowired  
    private CollectionService collectionService;  
  
    @Autowired  
    private HeldCollectionService heldCollectionService;  
  
    @Autowired  
    private CollectionInventoryRedisService collectionInventoryRedisService;  
  
    @Override  
    @Facade    public CollectionChainResponse chain(CollectionChainRequest request) {  
        Collection collection = collectionService.create(request);  
        ChainProcessRequest chainProcessRequest = new ChainProcessRequest();  
        chainProcessRequest.setIdentifier(request.getIdentifier());  
        chainProcessRequest.setClassId(request.getClassId());  
        chainProcessRequest.setClassName(request.getName());  
        chainProcessRequest.setBizType(ChainOperateBizTypeEnum.COLLECTION.name());  
        chainProcessRequest.setBizId(collection.getId().toString());  
        var chainRes = chainFacadeService.chain(chainProcessRequest);  
        CollectionChainResponse response = new CollectionChainResponse();  
        response.setSuccess(chainRes.getSuccess());  
        if (chainRes.getSuccess()) {  
            response.setCollectionId(collection.getId());  
        }  
        return response;  
    }  
  
    @Override  
    @Facade    public CollectionSaleResponse confirmSale(CollectionSaleRequest request) {  
        UserQueryRequest userQueryRequest = new UserQueryRequest();  
        userQueryRequest.setUserId(Long.valueOf(request.getUserId()));  
        CompletableFuture<UserQueryResponse<UserInfo>> queryUserFuture = CompletableFuture.supplyAsync(() -> userFacadeService.query(userQueryRequest));  
  
        CollectionConfirmSaleRequest confirmSaleRequest = new CollectionConfirmSaleRequest(request.getIdentifier(), request.getCollectionId(), request.getQuantity(), request.getBizNo(), request.getBizType(),request.getUserId());  
        CollectionConfirmSaleResponse confirmSaleResponse = collectionService.confirmSale(confirmSaleRequest);  
        CollectionSaleResponse response = new CollectionSaleResponse();  
  
        if (confirmSaleResponse.getSuccess()) {  
            Collection collection = confirmSaleResponse.getCollection();  
            HeldCollection heldCollection = confirmSaleResponse.getHeldCollection();  
  
            Thread.ofVirtual().start(() -> {  
                ChainProcessRequest chainProcessRequest = new ChainProcessRequest();  
                chainProcessRequest.setRecipient(getUserInfo(queryUserFuture).getBlockChainUrl());  
                chainProcessRequest.setClassId(collection.getClassId());  
                chainProcessRequest.setClassName(collection.getName());  
                chainProcessRequest.setSerialNo(heldCollection.getSerialNo());  
                chainProcessRequest.setBizId(heldCollection.getId().toString());  
                chainProcessRequest.setBizType(ChainOperateBizTypeEnum.HELD_COLLECTION.name());  
                chainProcessRequest.setIdentifier(confirmSaleResponse.getHeldCollection().getId().toString());  
                chainFacadeService.mint(chainProcessRequest);  
            });  
  
            response.setSuccess(true);  
            response.setHeldCollectionId(heldCollection.getId());  
        } else {  
            response.setSuccess(false);  
            response.setResponseCode(confirmSaleResponse.getResponseCode());  
            response.setResponseMessage(confirmSaleResponse.getResponseMessage());  
        }  
  
        return response;  
    }  
  
    @Override  
    @Facade    public CollectionSaleResponse trySale(CollectionSaleRequest request) {  
        CollectionTrySaleRequest collectionTrySaleRequest = new CollectionTrySaleRequest(request.getIdentifier(),request.getCollectionId(),request.getQuantity());  
        Boolean trySaleResult = collectionService.trySale(collectionTrySaleRequest);  
        CollectionSaleResponse response = new CollectionSaleResponse();  
        response.setSuccess(trySaleResult);  
        return response;  
    }  
  
    @Override  
    @Facade    public CollectionSaleResponse cancelSale(CollectionSaleRequest request) {  
        CollectionCancelSaleRequest collectionCancelSaleRequest = new CollectionCancelSaleRequest(request.getIdentifier(), request.getCollectionId(),request.getQuantity());  
        Boolean cancelSaleResult = collectionService.cancelSale(collectionCancelSaleRequest);  
        CollectionSaleResponse response = new CollectionSaleResponse();  
  
        response.setSuccess(cancelSaleResult);  
        return response;  
    }  
  
    private UserInfo getUserInfo(CompletableFuture<UserQueryResponse<UserInfo>> queryUserFuture) {  
        UserQueryResponse<UserInfo> userQueryResponse;  
  
        try {  
            userQueryResponse = queryUserFuture.get();  
            if (!userQueryResponse.getSuccess() || null == userQueryResponse.getData()) {  
                throw new CollectionException(COLLECTION_USER_QUERY_FAIL);  
            }  
            return userQueryResponse.getData();  
        } catch (InterruptedException | ExecutionException e) {  
            throw new RuntimeException(e);  
        }  
    }  
  
    @Override  
    @Facade    public CollectionTransferResponse transfer(CollectionTransferRequest request) {  
        UserQueryRequest buyerQuery = new UserQueryRequest();  
        buyerQuery.setUserId(request.getBuyerId());  
        var buyerRes = userFacadeService.query(buyerQuery);  
        UserQueryRequest sellerQuery = new UserQueryRequest();  
        sellerQuery.setUserId(request.getSellerId());  
        var sellerRes = userFacadeService.query(sellerQuery);  
        if (!buyerRes.getSuccess() || null == buyerRes.getData() || !sellerRes.getSuccess()  
                || null == sellerRes.getData()) {  
            throw new CollectionException(COLLECTION_USER_QUERY_FAIL);  
        }  
        Collection collection = collectionService.getById(request.getCollectionId());  
        if (null == collection) {  
            throw new CollectionException(COLLECTION_QUERY_FAIL);  
        }  
        HeldCollection heldCollection = heldCollectionService.getById(request.getHeldCollectionId());  
        if (null == heldCollection || StringUtils.isNotBlank(heldCollection.getNftId())) {  
            throw new CollectionException(HELD_COLLECTION_QUERY_FAIL);  
        }  
        ChainProcessRequest chainProcessRequest = new ChainProcessRequest();  
        chainProcessRequest.setRecipient(buyerRes.getData().getBlockChainUrl());  
        chainProcessRequest.setOwner(sellerRes.getData().getBlockChainUrl());  
        chainProcessRequest.setClassId(collection.getClassId());  
        chainProcessRequest.setIdentifier(request.getIdentifier());  
        chainProcessRequest.setNtfId(heldCollection.getNftId());  
        var transferRes = chainFacadeService.transfer(chainProcessRequest);  
        CollectionTransferResponse response = new CollectionTransferResponse();  
        response.setSuccess(transferRes.getSuccess());  
        if (transferRes.getSuccess()) {  
            //更新藏品持有表  
            HeldCollectionTransferRequest heldCollectionTransferRequest = new HeldCollectionTransferRequest();  
            BeanUtils.copyProperties(request, heldCollectionTransferRequest);  
            var newHeldCollection = heldCollectionService.transfer(heldCollectionTransferRequest);  
            response.setHeldCollectionId(newHeldCollection.getId());  
        }  
        return response;  
    }  
  
    @Override  
    @Facade    public CollectionDestroyResponse destroy(CollectionDestroyRequest request) {  
        Collection collection = collectionService.getById(request.getCollectionId());  
        if (null == collection) {  
            throw new CollectionException(COLLECTION_QUERY_FAIL);  
        }  
        HeldCollection heldCollection = heldCollectionService.getById(request.getHeldCollectionId());  
        if (null == heldCollection || StringUtils.isNotBlank(heldCollection.getNftId())) {  
            throw new CollectionException(HELD_COLLECTION_QUERY_FAIL);  
        }  
        UserQueryRequest userQueryRequest = new UserQueryRequest();  
        userQueryRequest.setUserId(Long.valueOf(heldCollection.getUserId()));  
        var userRes = userFacadeService.query(userQueryRequest);  
        if (!userRes.getSuccess() || null == userRes.getData()) {  
            throw new CollectionException(COLLECTION_USER_QUERY_FAIL);  
        }  
        ChainProcessRequest chainProcessRequest = new ChainProcessRequest();  
        chainProcessRequest.setIdentifier(request.getIdentifier());  
        chainProcessRequest.setNtfId(heldCollection.getNftId());  
        chainProcessRequest.setClassId(collection.getClassId());  
        chainProcessRequest.setOwner(userRes.getData().getBlockChainUrl());  
  
        var destroyRes = chainFacadeService.destroy(chainProcessRequest);  
        CollectionDestroyResponse response = new CollectionDestroyResponse();  
        response.setSuccess(destroyRes.getSuccess());  
        if (destroyRes.getSuccess()) {  
            //更新藏品持有表  
            HeldCollectionDestroyRequest heldCollectionDestroyRequest = new HeldCollectionDestroyRequest();  
            BeanUtils.copyProperties(request, heldCollectionDestroyRequest);  
            var newHeldCollection = heldCollectionService.destroy(heldCollectionDestroyRequest);  
            response.setHeldCollectionId(newHeldCollection.getId());  
        }  
        return response;  
    }  
  
    @Override  
    @Facade    public SingleResponse<CollectionVO> queryById(Long collectionId) {  
        Collection collection = collectionService.queryById(collectionId);  
  
        CollectionInventoryRequest request = new CollectionInventoryRequest();  
        request.setCollectionId(collectionId.toString());  
        Integer inventory = collectionInventoryRedisService.getInventory(request);  
  
        if (inventory == null) {  
            inventory = 0;  
        }  
  
        CollectionVO collectionVO = CollectionConvertor.INSTANCE.mapToVo(collection);  
        collectionVO.setInventory(inventory.longValue());  
        collectionVO.setState(collection.getState(), collection.getSaleTime(), inventory.longValue());  
        return SingleResponse.of(collectionVO);  
    }  
  
    @Override  
    @Facade    public SingleResponse<Boolean> preInventoryDeduct(Long collectionId, int quantity, String identifier) {  
  
        CollectionInventoryRequest request = new CollectionInventoryRequest();  
        request.setIdentifier(identifier);  
        request.setInventory(quantity);  
        request.setCollectionId(collectionId.toString());  
        CollectionInventoryResponse collectionInventoryResponse = collectionInventoryRedisService.decrease(request);  
        if (collectionInventoryResponse.getSuccess()) {  
  
//            //通过netty时间轮，起一个延迟任务，反查订单是否创建成功，如果未创建，则回退库存  
//            HashedWheelTimer timer = new HashedWheelTimer();  
//  
//            TimerTask task = new TimerTask() {  
//                @Override  
//                public void run(Timeout timeout) throws Exception {  
//                    //但是这里不做实现了，主要原因：  
//                    //1、藏品模块或者库存模块，不应该冗余太多业务关于订单模块的业务逻辑，  
//                    //2、这种检查并不能100%的解决问题，因为一旦这时候服务挂了，或者应用重启了，就丢失了这个任务了。  
//                    //所以，这种不一致的情况需要通过对账实现  
//                    //要么就是单独搞一个对账中心，要么就是通过对账平台配核对来发现。  
//                }  
//            };  
//  
//            //timer.newTimeout(task, 3, TimeUnit.MINUTES);  
  
            return SingleResponse.of(true);  
        }  
  
        logger.error("decrease inventory failed, " + JSON.toJSONString(collectionInventoryResponse));  
        return SingleResponse.fail(collectionInventoryResponse.getResponseCode(), collectionInventoryResponse.getResponseMessage());  
    }  
  
    @Override  
    public SingleResponse<CollectionInventoryVO> queryInventory(Long collectionId) {  
        Collection collection = collectionService.queryById(collectionId);  
  
        CollectionInventoryVO collectionInventoryVO = new CollectionInventoryVO();  
        collectionInventoryVO.setQuantity(collection.getQuantity());  
        collectionInventoryVO.setOccupiedInventory(collection.getOccupiedInventory());  
  
        CollectionInventoryRequest collectionInventoryRequest = new CollectionInventoryRequest();  
        collectionInventoryRequest.setCollectionId(collectionId.toString());  
        Integer saleableInventory = collectionInventoryRedisService.getInventory(collectionInventoryRequest);  
        collectionInventoryVO.setSaleableInventory(saleableInventory.longValue());  
        return SingleResponse.of(collectionInventoryVO);  
    }  
  
    @Override  
    public PageResponse<CollectionVO> pageQuery(CollectionPageQueryRequest request) {  
        PageResponse<Collection> colletionPage = collectionService.pageQueryByState(request.getKeyword(), request.getState(), request.getCurrentPage(), request.getPageSize());  
        return PageResponse.of(CollectionConvertor.INSTANCE.mapToVo(colletionPage.getDatas()), colletionPage.getTotal(), colletionPage.getPageSize());  
    }  
  
    @Override  
    public PageResponse<HeldCollectionVO> pageQueryHeldCollection(HeldCollectionPageQueryRequest request) {  
        Page<HeldCollection> colletionPage = heldCollectionService.pageQueryByState(request.getUserId(), request.getState(), request.getCurrentPage(), request.getPageSize());  
        return PageResponse.of(HeldCollectionConvertor.INSTANCE.mapToVo(colletionPage.getRecords()), (int) colletionPage.getTotal(), request.getPageSize());  
    }  
  
    @Override  
    public SingleResponse<HeldCollectionVO> queryByNftId(String nftId) {  
        HeldCollection transferCollection = heldCollectionService.queryByNftIdAndState(nftId, HeldCollectionState.ACTIVED.name());  
        return SingleResponse.of(HeldCollectionConvertor.INSTANCE.mapToVo(transferCollection));  
    }  
}
```