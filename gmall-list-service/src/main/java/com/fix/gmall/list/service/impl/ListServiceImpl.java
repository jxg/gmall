package com.fix.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.fix.gmall.bean.SkuLsInfo;
import com.fix.gmall.bean.SkuLsParams;
import com.fix.gmall.bean.SkuLsResult;
import com.fix.gmall.config.RedisUtil;
import com.fix.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ListServiceImpl implements ListService {
    @Autowired
    private JestClient jestClient;

    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";
    @Autowired
    private RedisUtil redisUtil;
    @Override
    public void incrHotScore(String skuId) {
        Jedis jedis = redisUtil.getJedis();
        String hotKey = "hotScore";
        Double count = jedis.zincrby(hotKey, 1, "skuId:" + skuId);
        // 按照一定的规则，来更新es
        if (count%10==0){
            // 则更新一次es
            // es 更新语句  Math.round(12.5) 13  Math.round(-12.5) -12
            updateHotScore(skuId,  Math.round(count));
        }


    }

    private void updateHotScore(String skuId, long hotScore) {
        String upd="{\n" +
                "  \"doc\": {\n" +
                "     \"hotScore\": "+hotScore+"\n" +
                "  }\n" +
                "}";
        Update update = new Update.Builder(upd).index(ES_INDEX).type(ES_TYPE).id(skuId).build();

        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo) {
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE)
                .id(skuLsInfo.getId())
                .build();

        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public SkuLsResult search(SkuLsParams skuLsParams){
         String query = makeQueryStringForSearch(skuLsParams);
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult = null;

        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        SkuLsResult skuLsResult = makeResultForSearch(searchResult,skuLsParams);
        return skuLsResult;
    }

    private SkuLsResult makeResultForSearch(SearchResult searchResult, SkuLsParams skuLsParams) {
        // 声明对象
        SkuLsResult skuLsResult = new SkuLsResult();
        ArrayList<SkuLsInfo> skuLsInfoArrarList = new ArrayList<>();
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
       for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
           SkuLsInfo skuLsInfo = hit.source;

           if (hit.highlight!=null && hit.highlight.size()>0){
               Map<String, List<String>> highlight = hit.highlight;
               List<String> list = highlight.get("skuName");
               String skuNameHI = list.get(0);
               skuLsInfo.setSkuName(skuNameHI);
           }
           skuLsInfoArrarList.add(skuLsInfo);
       }
       skuLsResult.setSkuLsInfoList(skuLsInfoArrarList);
       skuLsResult.setTotal(searchResult.getTotal());
       long totalPages = (searchResult.getTotal()+skuLsParams.getPageSize()-1) / skuLsParams.getPageSize();
       skuLsResult.setTotalPages(totalPages);

        // 声明一个集合来存储平台属性值Id
        ArrayList<String> stringArrayList = new ArrayList<>();
        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        for (TermsAggregation.Entry bucket : buckets) {
            String valueId = bucket.getKey();
            stringArrayList.add(valueId);
        }
        skuLsResult.setAttrValueIdList(stringArrayList);
        return skuLsResult;


    }

    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //hightlight
        if(skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length() > 0){
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            boolQueryBuilder.must(matchQueryBuilder);
            HighlightBuilder highlighter = searchSourceBuilder.highlighter();
            highlighter.field("skuName");
            highlighter.preTags("<span style=color:red>");
            highlighter.postTags("</span>");
            searchSourceBuilder.highlight(highlighter);
        }
        // 判断平台属性值Id
       if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            for(String valueId : skuLsParams.getValueId()){
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                 boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        // 判断 三级分类Id
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            boolQueryBuilder.filter(termQueryBuilder);
        }

       searchSourceBuilder.query(boolQueryBuilder);

        int from =(skuLsParams.getPageNo() -1) * skuLsParams.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(skuLsParams.getPageSize());

        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr");
        groupby_attr.field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);

        String query = searchSourceBuilder.toString();
        System.out.println("query:="+query);
        return query;



    }


}
