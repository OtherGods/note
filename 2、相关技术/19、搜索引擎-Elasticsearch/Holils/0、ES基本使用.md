# 1、索引库（MySQL表）CURD

1. 创建指定索引库：`PUT /索引库名`
   创建示例：
	```json
	PUT /heima
	{
		"mappings": {
			"properties": {
			  "info":{
				"type": "text",
				"analyzer": "ik_smart"
			  },
			  "email":{
				"type": "keyword",
				"index": "false"
			  },
			  "name":{
				"properties": {
				  "firstName": {
					"type": "keyword"
				  }
				}
			  }
			}
		}
	}
	```
2. 删除指定索引库：`DELETE /索引库名`
   删除示例：
	```json
	DELETE /heima
	```
3. 添加字段：`PUT /索引库名/_mapping`
   添加示例：
	```json
	PUT /heima/_mapping
	{
		"properties": {
			"新字段名":{
			  "type": "integer"
			}
		}
	}
	```
4. 查询指定索引库：`GET /索引库名`
   查询示例：
	```json
	GET /heima
	
	// 查询heima索引库的结构
	GET /heima/_mapping
	```

# 2、文档（MySQL表中一条记录）CURD

1. 创建指定文档：`POST /{索引库名}/_doc/文档id   { json文档 }`
   创建示例：
	```json
	POST /heima/_doc/1
	{
	    "info": "黑马程序员Java讲师",
	    "email": "zy@itcast.cn",
	    "name": {
	        "firstName": "云",
	        "lastName": "赵"
	    }
	}
	```
2. 删除指定文档：`DELETE /{索引库名}/_doc/文档id`
   删除示例：
	```json
	DELETE /heima/_doc/1
	```
3. 修改指定文档：
  - 全量修改：`PUT /{索引库名}/_doc/文档id { json文档 }`；全量修改是**覆盖原来的文档**，其本质是：根据指定的id删除文档、新增一个相同id的文档
    全量修改示例：
	```json
	PUT /heima/_doc/1
	{
	    "info": "黑马程序员高级Java讲师",
	    "email": "zy@itcast.cn",
	    "name": {
	        "firstName": "云",
	        "lastName": "赵"
	    }
	}
	```
  - 增量修改：`POST /{索引库名}/_update/文档id { "doc": {字段}}`
    增量修改示例：
	```json
	POST /heima/_update/1
	{
		"doc": {
			"email": "ZhaoYun@itcast.cn"
		}
	}
	```
4. 查询指定文档：`GET /{索引库名}/_doc/文档id`
   查询示例：
	```json
	GET /heima/_doc/1
	```

# 3、高级查询

**==通用格式如下，其他查询与通用格式类似，只是查询类型和查询条件发生了变化==**
```json
GET /indexName/_search
{
	"query": {
		"查询类型": {
		  "查询条件": "条件值"
		}
	}
}
```

索引库结构：
```json
PUT /hotel
{
	"mappings": {
		"properties": {
			"id": {
				"type": "keyword"
			},
			"name":{
				"type": "text",
				"analyzer": "ik_max_word",
				"copy_to": "all"
			},
			"brand":{
				"type": "keyword",
				"copy_to": "all"
			},
			"city":{
				"type": "keyword",
				"copy_to": "all"
			},
			"all":{
				"type": "text",
				"analyzer": "ik_max_word"
			},
			"address":{
				"type": "keyword",
				"index": false
			},
			"price":{
				"type": "integer"
			},
			"score":{
				"type": "integer"
			},
			"starName":{
				"type": "keyword"
			},
			"business":{
				"type": "keyword"
			},
			"location":{
				"type": "geo_point"
			},
			"pic":{
				"type": "keyword",
				"index": false
			}
		}
	}
}
```

## 3.1、查询指定索引下的所有文档

```json
GET /hotel/_search
{
	"query": {
		"match_all": {}
	}
}
```

## 3.2、全文检索（full text）查询

用**分词器**对输入内容**分词**后，根据**词条从倒排索引中查询**，按照**或**的逻辑拼接查询结果。

索引库结构部分定义如下：
```json
PUT /hotel
{
	"mappings": {
		"properties": {
			"name":{
				"type": "text",
				"analyzer": "ik_max_word",
				"copy_to": "all"
			},
			"brand":{
				"type": "keyword",
				"copy_to": "all"
			},
			"city":{
				"type": "keyword",
				"copy_to": "all"
			},
			"all":{
				"type": "text",
				"analyzer": "ik_max_word"
			},
			…………
		}
	}
}
```

全文检索分为单字段查询和多字段查询，以下两个DSL的查询结果是一样的
1. 单字段查询：
	```json
	GET /hotel/_search
	{
		"query": {
			"match": {
			  "all": "地区"
			}
		}
	}
	```
2. 多字段查询：
	```json
	GET /hotel/_search
	{
		"query": {
			"multi_match": {
			  "query": "地区", 
			  "fields": ["name","brand","city"]
			}
		}
	}
	```

## 3.3、精确查询

精确查询一般是查找keyword、数值、日期、boolean等类型字段。所以**不会对搜索条件分词**。常见的有：
1. term查询：根据词条精确匹配，一般搜索keyword类型、数值类型、布尔类型、日期类型字段
	```json
	GET /hotel/_search
	{
		"query": {
			"term": {
				"city": {
					"value": "上海"
				}
			}
		}
	}
	```
2. range查询：根据数值范围查询，可以是数值、日期的范围
	```json
	GET /hotel/_search
	{
		"query":  {
			"range":  {
				"price":  {
					"gte":  1000,
					"lte":  1100
				}
			}
		}
	}
	```

## 3.4、地理查询

地理坐标查询，其实就是根据经纬度查询，分为两种：
1. geo_bounding_box（矩形范围查询）：查询矩形内部的数据
   示例：查询左上点到右下点包含的矩形内的数据
	```json
	GET /indexName/_search
	{
		"query": {
			"geo_bounding_box": {
				"location": { //具体字段
					"top_left": { // 左上点经纬度
					  "lat": 31.1,
					  "lon": 121.5
					},
					"bottom_right": { // 右下点经纬度
					  "lat": 30.9,
					  "lon": 121.7
					}
				}
			}
		}
	}
	```
2. geo_distance（距离查询）：查询圆形内部的数据
   示例：查询指定圆心、半径内的数据
	```json
	GET  /indexName/_search
	{
		"query":  {
			"geo_distance":  {
				"distance":  "15km", // 半径
				"location":  "31.21,121.5" //具体字段:经纬度
			}
		}
	}
	```

## 3.5、复合查询

复合（compound）查询：可以将上述各种查询条件组合起来，合并查询条件，实现更复杂的搜索逻辑。常见的有两种：
1. fuction score（算分函数查询）：可以控制文档相关性算分，控制文档排名，当我们利用match查询时，文档结果会根据与搜索词条的关联度打分（`_score`），返回结果时按照`_score`分值降序排列
   ![image-20210721191544750.png](https://raw.githubusercontent.com/OtherGods/MaterialImage/main/img/202510122309117.png)
	**==functions不是必须的==**
	function score 查询中包含四部分内容：
	- **原始查询**条件：query部分，基于这个条件搜索文档，并且基于BM25算法给文档打分，**原始算分**（query score)
	- **过滤条件**：filter部分，符合该条件的文档才会重新算分
	- **算分函数**：符合filter条件的文档要根据这个函数做运算，得到的**函数算分**（function score），有四种函数
	  - weight：函数结果是常量
	  - field_value_factor：以文档中的某个字段值作为函数结果
	  - random_score：以随机数作为函数结果
	  - script_score：自定义算分函数算法
	- **运算模式**：算分函数的结果、原始查询的相关性算分，两者之间的运算方式，包括：
	  - multiply：相乘
	  - replace：用function score替换query score
	  - 其它，例如：sum、avg、max、min
	示例：
	```json
	GET /hotel/_search
	{
	  "query": {
		"function_score": {
		  "query": {
			"match": {
			  "all": "外滩"
			}
		  },
		  "functions": [
			{
			  "filter": {
				"term": {
				  "brand": "如家"
				}
			  },
			  "weight": 10
			}
		  ],
		  "boost_mode": "sum"
		}
	  }
	}
	```
1. bool query（布尔查询）：布尔查询是一个或多个查询子句的组合，每一个子句就是一个**子查询**。子查询的组合方式有：
	- must：必须匹配每个子查询，类似“与”
	- should：选择性匹配子查询，类似“或”
	- must_not：必须不匹配，**不参与算分**，类似“非”
	- filter：必须匹配，**不参与算分**
	 DSL查询结果是must、must_not、filter查询子局的并集，should是非必须的，下面这个DSL的含义是：必须同时满足`city = "上海"` （来自 must）、`price > 500` （来自 must_not 的反向条件）、`score >= 45` （来自 filter）这三个条件，`brand = "皇冠假日"` 或 `brand = "华美达"`是非必须的（额外加分项）
	```json
	GET /hotel/_search
	{
		"query": {
			"bool": {
				"must": [
					{"term": {"city": "上海" }}
				],
				"should": [
					{"term": {"brand": "皇冠假日" }},
					{"term": {"brand": "华美达" }}
				],
				"must_not": [
					{ "range": { "price": { "lte": 500 } }}
				],
				"filter": [
					{ "range": {"score": { "gte": 45 } }}
				]
			}
		}
	}
	```

## 3.6、排序

elasticsearch **==默认按照相关度算分（`_score`）来降序方式排序==**，但是也支持自定义方式对搜索[结果排序](https://www.elastic.co/guide/en/elasticsearch/reference/current/sort-search-results.html)。可以排序字段类型有：keyword类型、数值类型、地理坐标类型、日期类型等。

1. keyword、数值、日期类型排序语法一致；按照声明的顺序，当第一个条件相等时，再按照第二个条件排序，以此类推
   示例：
	```json
	GET /hotel/_search
	{
	  "query": {
	    "match_all": {}
	  },
	  "sort": [
	    {
	      "score": "desc"
	    },
	    {
	      "price": "asc"
	    }
	  ]
	}
	```
2. 地理坐标排序
	```json
	GET /hotel/_search
	{
	  "query": {
	    "match_all": {}
	  },
	  "sort": [
	    {
	      "_geo_distance" : {
	          "location" : "31.1,121.5", // 文档中geo_point类型的字段名、目标坐标点
	          "order" : "asc", // 排序方式
	          "unit" : "km" // 排序的距离单位
	      }
	    }
	  ]
	}
	```

## 3.7、分页

```json
GET /hotel/_search
{
  "query": {
    "match_all": {}
  },
  "from": 0, // 分页开始的位置，默认为0
  "size": 10, // 期望获取的文档总数
}
```

scroll分页（使用快照存储排好序的结果集）：
```json
// 开始一个scroll时，es会保持搜索时刻的数据快照，这意味着即使数据随后被修改，返回的结果仍然是一致的
// 默认排序方式按照相关性算分（查询结果中的_score）从高到低排
GET /hotel/_search?scroll=1m
{
  "size": 10,  // 每页10条记录
  "query": {
    "match_all": {}
  }
}

GET /_search/scroll
{
	"scroll": "1m",
	"scroll_id": "_scroll_id"
}
```

```json
// 这个查询按 timestamp 字段排序，并在相同 timestamp 的情况下按 id 排序。
GET /hotel/_search
{
  "size": 10,
  "query": {
    "match_all": {}
  },
  "sort": [
    {"timestamp": "asc"},
    {"id": "asc"}
  ]
}

// 在后续的查询中，使用上一次查询结果中最后一条记录的排序值
GET /your_index/_search
{
  "size": 10,
  "query": {
    "match_all": {}
  },
  "sort": [
    {"timestamp": "asc"},
    {"id": "asc"}
  ],
  "search_after": [1609459200000, 10000]
}
```

## 3.8、高亮

**==高亮一定要使用全文检索==**，高亮的字段与搜索字段一致
```json
GET /hotel/_search
{
  "query": {
    "match": {
      "FIELD": "TEXT" // 查询条件，高亮一定要使用全文检索查询
    }
  },
  "highlight": {
    "fields": { // 指定要高亮的字段
      "FIELD": {
        "pre_tags": "<em>",  // 用来标记高亮字段的前置标签
        "post_tags": "</em>" // 用来标记高亮字段的后置标签
      }
    }
  }
}
```



