package com.atguigu.tingshu.model.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.CompletionField;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.suggest.Completion;

// 自动补全索引库
@Data
@Document(indexName = "suggestinfo")
@JsonIgnoreProperties(ignoreUnknown = true)//目的：防止json字符串转成实体对象时因未识别字段报错
public class SuggestIndex {


    /*悲惨世界*/

    @Id
    private String id;

    /*
       专辑名称，主播名称，用于给用户展示提词  悲惨世界
    * */
    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;


    /**
     * 用与检索建议词查询字段 汉字 悲 惨 世 界
     */
    @CompletionField(analyzer = "standard", searchAnalyzer = "standard", maxInputLength = 20)
    private Completion keyword;

    /**
     * 用与检索建议词查询字段 完整汉语拼音 beicanshijie
     */
    @CompletionField(analyzer = "standard", searchAnalyzer = "standard", maxInputLength = 20)
    private Completion keywordPinyin;

    /**
     * 用与检索建议词查询字段 完整汉字拼音首字母 bcsj
     */
    @CompletionField(analyzer = "standard", searchAnalyzer = "standard", maxInputLength = 20)
    private Completion keywordSequence;

}
