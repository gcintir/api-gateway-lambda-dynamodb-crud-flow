# api-gateway-lambda-dynamodb-crud-flow
Simple CRUD API serverless project with API Gateway, Lambda and DynamoDB services

curl --location --request POST 'https://jsmulddoee.execute-api.us-east-1.amazonaws.com/product' \
--header 'Content-Type: application/json' \
--data-raw '{
    "productId": "p-1",
    "title": "sport shoes",
    "category": "Sports Wear",
    "price": 700.50,
    "allowedCreditCards": ["a-bank", "b-bank"],
    "existingSizes": [32, 33, 37],
    "stockAmount": 20,
    "active": true,
    "features": {
        "colour": "black",
        "material": "textile"
    }
}'


curl --location --request PUT 'https://jsmulddoee.execute-api.us-east-1.amazonaws.com/product' \
--header 'Content-Type: application/json' \
--data-raw '{
    "productId": "p-1",
    "title": "sport shoes",
    "category": "Sports Wear",
    "price": 700.50,
    "allowedCreditCards": ["a-bank", "b-bank"],
    "existingSizes": [32, 33, 37],
    "stockAmount": 25,
    "active": true,
    "features": {
        "colour": "white",
        "material": "textile"
    }
}'


curl --location --request GET 'https://jsmulddoee.execute-api.us-east-1.amazonaws.com/product?productId=p-1'


curl --location --request DELETE 'https://jsmulddoee.execute-api.us-east-1.amazonaws.com/product/p-1'

curl --location --request PUT 'https://jsmulddoee.execute-api.us-east-1.amazonaws.com/product/amount/decrement/productId/p-1/value/1'