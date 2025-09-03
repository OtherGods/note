

[TOC]


## 1.什么是RBAC权限管理模型

我们在做任何一款产品的时候，或多或少都会涉及到用户和权限的问题。譬如，做企业类软件，不同部门、不同职位的人的权限是不同的；做论坛类产品的时候，版主和访客权限也是不一样的；再例如一款产品的收费用户和免费用户权限也是迥然不同的。

但在设计产品的用户和权限的关系的时候，很多产品经理可能按照感觉来，在并不清楚用户和权限是否存在优秀的理论模型的时候，就按照自我推理搭建了产品的用户和权限模型。而这种基于感觉和推理的模型肯定是有诸多问题的，譬如写死了关系导致权限不够灵活、考虑不周导致权限覆盖能力弱等等。

正如牛顿所言，站在巨人的肩膀上才能看的更远。我们不妨去参照已有的比较成熟的权限模型，如：RBAC（Role-Based Access Control）——基于角色的访问控制。我搜集了网上很多关于RBAC的资料，大多与如何用数据表实现RBCA相关，并不容易理解。所以，我会以产品经理的角度去解析RBAC模型，并分别举例如何运用这套已得到验证的成熟模型。

- **用户**：系统接口及访问的操作者
- **权限**：能够访问某接口后者做某种操作的授权资格
- **角色**：具有一类相同操作权限的用户的总称，可以理解为一定数量的权限的集合，权限的载体
## 2.RBAC权限管理系统

![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508231627611.png)

主要有5张表，**==用户表==** 存放用户信息，**==角色表==** 存放角色信息，**==权限表==** 存放权限信息，**==用户表和角色表的关联表==** 存放用户对应的角色，**==角色表和权限表的关联表==** 存放角色对应的权限
1. **部门表**
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508231627141.png)

2. **用户表**
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508231628307.png)

3. **角色表**
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508231628463.png)

4. **权限表**
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508231629214.png)
5. **用户-角色表**、**角色-权限表**
   ![image.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202508231630511.png)
