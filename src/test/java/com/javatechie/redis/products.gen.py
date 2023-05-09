#loop for from 1 to 10000, generate 10000 products
import requests

# Path: src/test/java/com/javatechie/redis/products.gen.py


for i in range(1,10000):
    #post request to add product with url localhost:8888/product with json data {"id": i, "name": "abc", "qty": 4, "price": 5000}
    response = requests.post('http://localhost:8888/product', json={"id": i, "name": "abc", "qty": 4, "price": 5000})
    #print response with id
    print(response.json()['id'])


