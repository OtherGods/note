# 典型回答

K是Key的意思，就是代表约束的，所以PK、UK这些都是代表不同类型的约束：
- PK：Primary Key ，主键约束
- UK：Unique Key， 唯一约束
- CK： check()， 检查约束
	- 限制某个字段的取值范围，确保数据符合某种业务规则
	- **MySQL 8.0.16+** 正式支持 `CHECK` 约束（之前版本语法上支持，但不会生效）
- FK：Foreign Key， 外键约束
- DF：default ，默认值，字段无之后用该值填充
