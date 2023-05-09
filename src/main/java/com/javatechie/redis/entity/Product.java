package com.javatechie.redis.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@RedisHash("Product")
public class Product implements Serializable {
    @Id
    private int id;
    private String name;
    private int qty;
    private long price;
    // //write toString() method for Product
    // public String getStr() {
    //     return "Product [id=" + id + ", name=" + name + ", qty=" + qty + ", price=" + price + "]";
    // }

    // //write setter and getter methods for Product
    // public int getId() {
    //     return id;
    // }
    // public void setId(int id) {
    //     this.id = id;
    // }
    // public String getName() {
    //     return name;
    // }
    // public void setName(String name) {
    //     this.name = name;
    // }
    // public int getQty() {
    //     return qty;
    // }
    // public void setQty(int qty)
    // {
    //     this.qty = qty;
    // }
}
