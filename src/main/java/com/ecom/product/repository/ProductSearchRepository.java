package com.ecom.product.repository;

import com.ecom.product.dto.ProductSearchFilter;
import com.ecom.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;


@Repository
@RequiredArgsConstructor
public class ProductSearchRepository {

    private final MongoTemplate mongoTemplate;


    // Allowed Sort Fields

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("price", "createAt", "averageRating", "name");



    // search() method builds and execute a dynamic query based on whichever filters are present in ProductSearchFiilter, then returns matching products with pagination applied

    // @return the matching Product documents for the requested page

    public List<Product> search(ProductSearchFilter filter) {

//        System.out.println(filter.getMaxPrice().getClass().getName());
//        System.out.println(filter.getMinPrice().getClass().getName());

        Query query = buildQuery(filter);

        System.out.println(query);

        query.with(buildPageable(filter));

        System.out.println(query);

        return mongoTemplate.find(query, Product.class);
    }



    // countSearch()

//    Runs the SAME filter criteria as search() but counts total matches WITHOUT pagination — needed to calculate totalPages/totalElementsfor PageResponse. We deliberately build the query criteria again here (not reusing the paginated Query object) because count queriesmust not have skip/limit/sort applied.

    public long countSearch(ProductSearchFilter filter){
        Query query = buildQuery(filter);
        return mongoTemplate.count(query, Product.class);
    }



    // buildQuery()

    //  The core dynamic query builder. Always includes active=true —customers should never see soft-deleted products through search,regardless of which other filters they apply.

    private Query buildQuery(ProductSearchFilter filter){

        List<Criteria> criteriaList = new java.util.ArrayList<>();

        criteriaList.add(Criteria.where("active").is(true));


        if(StringUtils.hasText(filter.getCategoryId())) {
            criteriaList.add(Criteria.where("categoryId").is(filter.getCategoryId()));
        }


        if(filter.getMinPrice() != null){
            criteriaList.add(Criteria.where("price").gte(filter.getMinPrice()));
        }


        if (filter.getMaxPrice() != null){
            criteriaList.add(Criteria.where("price").lte(filter.getMaxPrice()));
        }


        // product specific

//        if(StringUtils.hasText((filter.getFabric()))) {
//            criteriaList.add(Criteria.where("fabric").regex("^" + filter.getFabric() + "$", "i"));
//        }

        if(StringUtils.hasText((filter.getBrand()))) {
            criteriaList.add(Criteria.where("brand").regex("^" + filter.getBrand() + "$", "i"));
        }


        if(StringUtils.hasText(filter.getColor())) {
            criteriaList.add(Criteria.where("color").regex("^" + filter.getColor() + "$", "i"));
        }


//        if(StringUtils.hasText(filter.getOccasion())) {
//            criteriaList.add(Criteria.where("occasion").regex("^" + filter.getOccasion() + "$" , "i"));
//        }

        if(StringUtils.hasText(filter.getProductType())) {
            criteriaList.add(Criteria.where("productType").regex("^" + filter.getProductType() + "$" , "i"));
        }


        if(StringUtils.hasText(filter.getCondition())) {
            criteriaList.add(Criteria.where("condition").regex("^" + filter.getCondition() + "$" , "i"));
        }

        //


        Query query = new Query();

        query.addCriteria(new Criteria().andOperator(
                criteriaList.toArray(new Criteria[0])
        ));


        if(StringUtils.hasText(filter.getKeyword())) {
            TextCriteria textCriteria = TextCriteria
                    .forDefaultLanguage()
                    .matching(filter.getKeyword());
            query.addCriteria(textCriteria);
        }


        return query;
    }


    // buildPageable()

    private Pageable buildPageable(ProductSearchFilter filter) {
        String sortField = ALLOWED_SORT_FIELDS.contains(filter.getSortBy())
                ? filter.getSortBy()
                : "createdAt";

        Sort.Direction direction = "asc".equalsIgnoreCase(filter.getSortDir())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(filter.getPage(), filter.getSize(), Sort.by(direction, sortField));
    }
}
