package com.collegebox.webservice.jsonrpc.elasticsearch.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.collegebox.webservice.jsonrpc.elasticsearch.SearchService;
import com.collegebox.webservice.pojo.elasticsearch.College;
import com.collegebox.webservice.pojo.elasticsearch.CollegeResponse;

import net.sf.json.JSONObject;

@Service
public class SearchServiceImpl implements SearchService {
	
	private static final Log log = LogFactory.getLog(SearchServiceImpl.class);
	
	@Value("${collegebox.elasticsearch.host}")
	private String hostName;

	@SuppressWarnings("resource")
	@Override
	public CollegeResponse searchColleges(String text, int from, int size) throws Exception {
		// TODO Auto-generated method stub
		log.info("rpc request, method:searchColleges, param:String text=" + text);
		log.info("hostName=" + hostName);
		
		Settings settings = ImmutableSettings.settingsBuilder()  
                .put("client.transport.sniff", true)  
                .put("client", true)  
                .put("data",false)  
                .put("clusterName","elasticsearch")  
                .build();
		
		Client client = new TransportClient(settings)
		        .addTransportAddress(new InetSocketTransportAddress(hostName, 9300));
		
		try {
			
			QueryStringQueryBuilder queryBuilder = new QueryStringQueryBuilder("香港大学");
			queryBuilder.analyzer("ik").field("college_area").field("college_ranking")
			.field("college_intro").field("college_surrounding").field("school_intro")
			.field("major_intro").field("degree_requirements").useDisMax(true);
			
			SearchResponse response = client.prepareSearch("collegebox")
					.setTypes("college").setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
					.setQuery(queryBuilder).setFrom(from).setSize(size).setExplain(true)
					.addHighlightedField("college_area")
					.addHighlightedField("college_ranking")
					.addHighlightedField("college_intro")
					.addHighlightedField("college_surrounding")
					.addHighlightedField("school_intro")
					.addHighlightedField("major_intro")
					.addHighlightedField("degree_requirements")
					.setHighlighterEncoder("UTF-8")
					.setHighlighterPreTags("<em>")
					.setHighlighterPostTags("</em>")
			        .execute().actionGet();
			
			CollegeResponse resp = new CollegeResponse();
			
			SearchHits searchHits = response.getHits();
			resp.setTotal(searchHits.getTotalHits());
			resp.setMax_score(searchHits.getMaxScore());
			
			SearchHit[] hits = searchHits.getHits();
			List<College> respHits = new ArrayList<College>();
			
			for (SearchHit hit : hits) {
				
				College college = new College();
		    	college.setSource(JSONObject.fromObject(hit.getSourceAsString()));
		    	
		    	Map<String, HighlightField> map = hit.getHighlightFields();
		    	Map<String, List<String>> highlight = new HashMap<String, List<String>>();
		    	
		    	for (Map.Entry<String, HighlightField> entry : map.entrySet()) {
		    		
		    		HighlightField field = entry.getValue();
		    		List<String> list = new ArrayList<String>();
		    		
		    		Text[] texts = field.getFragments();
			    	for(Text fragment : texts) {
			    		list.add(fragment.string());
			    	}
			    	
			    	highlight.put(entry.getKey(), list);
		    		
		    	}
		    	
		    	college.setHighlight(highlight);
		    	respHits.add(college);
		    	
		    }
			
			resp.setHits(respHits);
			
			return resp;
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			client.close();
		}
	}

}
