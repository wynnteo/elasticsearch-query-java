package com.wynntech.services;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wynntech.model.Product;
import com.wynntech.repository.ProductRepository;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;

@Service
public class ProductService {
    private static final String INDEX = "products_v2";
    private static final String TYPE = "_doc";

    private RestHighLevelClient client;

    private final ObjectMapper objectMapper;

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(RestHighLevelClient client, ObjectMapper objectMapper, ProductRepository productRepository) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.productRepository = productRepository;
    }

    // Create Product
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    // Get Product
    public Product getProduct(Long id) {
        Optional<Product> optionalProduct = productRepository.findById(id);
        return optionalProduct.orElse(null);
    }

    // Update Product
    public Product updateProduct(Product product) {
        // Check if product exists in database
        if (productRepository.existsById(product.getId())) {
            return productRepository.save(product);
        }
        return null; // Handle not found case
    }

    // Delete Product
    public boolean deleteProduct(Long id) {
        // Check if product exists in database
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false; // Handle not found case
    }

    public List<Product> searchProductsByName(String name) throws IOException {
        System.out.println(name);
        SearchRequest searchRequest = new SearchRequest(INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery("name", name));
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println("Search response: " + searchResponse.toString());
        return mapSearchResponseToProducts(searchResponse);
    }

    // Filter Products by Price Range
    public List<Product> filterProductsByPriceRange(double minPrice, double maxPrice) throws IOException {
        SearchRequest searchRequest = new SearchRequest(INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.rangeQuery("price").gte(minPrice).lte(maxPrice));
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println("Search response: " + searchResponse.toString());
        return mapSearchResponseToProducts(searchResponse);
    }

    // Search Products by Name and Match by Category
    public List<Product> searchProductsByNameAndCategory(String searchTxt, String category) throws IOException {
        SearchRequest searchRequest = new SearchRequest(INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // Create a bool query for combining category, name, and description conditions
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must(QueryBuilders.matchQuery("category", category));

        // Create a should clause for name and description (either can match)
        BoolQueryBuilder nameOrDescShouldQuery = QueryBuilders.boolQuery()
            .should(QueryBuilders.wildcardQuery("name", "*" + searchTxt.toLowerCase() + "*"))
            .should(QueryBuilders.wildcardQuery("description", "*" + searchTxt.toLowerCase() + "*"));
        boolQuery.must(nameOrDescShouldQuery);

        searchSourceBuilder.query(boolQuery);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println("Search response: " + searchResponse.toString());
        return mapSearchResponseToProducts(searchResponse);
    }

    // Helper method to map SearchResponse to Product objects
    private List<Product> mapSearchResponseToProducts(SearchResponse searchResponse) {
        SearchHit[] searchHits = searchResponse.getHits().getHits();
        List<Product> products = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            products.add(objectMapper.convertValue(sourceAsMap, Product.class));
        }
        return products;
    }

}
