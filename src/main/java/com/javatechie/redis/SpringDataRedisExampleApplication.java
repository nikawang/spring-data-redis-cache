package com.javatechie.redis;

import com.javatechie.redis.entity.Product;
import com.javatechie.redis.respository.ProductDao;

import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Random;

@SpringBootApplication
@RestController
@RequestMapping("/product")
@EnableCaching
public class SpringDataRedisExampleApplication {

    private static final Logger logger = LoggerFactory.getLogger(SpringDataRedisExampleApplication.class);

    @Autowired
    private ProductDao dao;


    @PostMapping
    public Product save(@RequestBody Product product) {
        return dao.save(product);
    }

    @GetMapping
    public Product getRandomProducts() {
        
        Random randId = new Random();
        int randomNum = randId.nextInt(10000) + 1;
        logger.info("Get Product Id with \t" + randomNum);

        Random randOP = new Random();
        String randomString = getAlphaNumericString(200);
        if(randOP.nextInt(10) > 5)
        {
            logger.info("Read from Redis \t" + randomNum);
            logger.info("执行方法cacheable, 从redis查询");
            logger.info("======" + randomString + "======");
            String base64Text =  new String(Base64.encodeBase64(randomString.getBytes()));
            logger.info("======" + base64Text + "======");
            String decodedString = new String(Base64.decodeBase64(base64Text));
            
            logger.info("======" + decodedString + "======");
            Product product = dao.findProductById(randomNum);
            product.setQty((int)getPi());
            return product;
        }

        double pi = getPi();
        randomNum = (int)pi;
        
        return new Product(randomNum,randomString,randomNum,randomNum);
    }

    @GetMapping("/{id}")
    @Cacheable(key = "#id",value = "Product",unless = "#result.price > 1000")
    public Product findProduct(@PathVariable int id) {
        String randomString = getAlphaNumericString(200);
        logger.info("执行方法cacheable, 从redis查询");
        logger.info("======" + randomString + "======");
        String base64Text =  new String(Base64.encodeBase64(randomString.getBytes()));
        logger.info("======" + base64Text + "======");
        String decodedString = new String(Base64.decodeBase64(base64Text));
        logger.info("======" + decodedString + "======");
        // Product product = dao.findProductById(id);
        // product.setName(base64Text);
        // product.setQty(id);
        return dao.findProductById(id);
    }

    @DeleteMapping("/{id}")
    @CacheEvict(key = "#id",value = "Product")
    public String remove(@PathVariable int id) {
        logger.info("Deleted " + id + "\t Successfully");
        return dao.deleteProduct(id);
    }

    private static String getAlphaNumericString(int n)
    {
    
    // choose a Character random from this String
    String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "0123456789"
            + "abcdefghijklmnopqrstuvxyz";
    
    // create StringBuffer size of AlphaNumericString
    StringBuilder sb = new StringBuilder(n);
    
    for (int i = 0; i < n; i++) {
    
    // generate a random number between
    // 0 to AlphaNumericString variable length
    int index
        = (int)(AlphaNumericString.length()
        * Math.random());
    
    // add Character one by one in end of sb
    sb.append(AlphaNumericString
        .charAt(index));
    }
    
    return sb.toString();
    }


    private static double getPi()
    {
        int numPoints = 1000000;
        int numInsideCircle = 0;
        Random rand = new Random();

        for (int i = 0; i < numPoints; i++) {
        double x = rand.nextDouble();
        double y = rand.nextDouble();
        double distance = Math.sqrt(x * x + y * y);
        if (distance <= 1) {
            numInsideCircle++;
        }
        }

        double pi = 4.0 * numInsideCircle / numPoints;
        System.out.println("Estimated value of Pi: " + pi);
    return pi;
    }

    public static void main(String[] args) {
        SpringApplication.run(SpringDataRedisExampleApplication.class, args);

        // String str = "Product [id=1, name=TV, qty=10, price=1000]";

        // System.out.println(new String(Base64.encodeBase64(str.getBytes())));
    }



}
