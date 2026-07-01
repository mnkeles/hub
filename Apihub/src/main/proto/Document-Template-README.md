# togg-ms-protos documantation template

## Getting started
This template provides a standardized format for documenting gRPC services.
By following this template, you can ensure consistency and clarity across all service documentation.

# Overview
Provide a brief overview of the service, including its purpose and main functionalities.

## Business Rules
The Business Rules section is crucial for outlining the operational constraints, conditions, and logic 
that must be adhered to when interacting with the service. This section ensures that users understand the necessary prerequisites, 
authentication requirements, and any specific conditions that must be met for successful operation.

    ## What to Include in the Business Rules Section
     -- Authentication and Authorization:
        Explain the authentication mechanisms required for accessing the service.
        Specify any roles or permissions needed to access or perform certain operations.
        Detail the use of tokens or credentials, including special roles like "impersonator."
    
     -- Data Validation:
        Describe any validation rules that apply to the input data.
        Specify required fields and data formats for requests.
    
     -- Operational Constraints:
        Describe any operational constraints or business logic that must be followed.
        Include any conditions under which certain operations are permitted or restricted

## Error Handling;
Define the types of errors that can occur and their corresponding codes.
Describe the conditions under which specific errors are returned (e.g., validation errors, permission errors, unexpected internal errors)


## Generate md doc steps on Linux;

    protobuf install : https://github.com/pseudomuto/protoc-gen-doc/releases
    
    1- download the latest version  : 
    wget https://github.com/pseudomuto/protoc-gen-doc/releases/download/v1.5.1/protoc-gen-doc-1.5.1.linux-amd64.go1.15.6.tar.gz
    tar -xvf protoc-gen-doc-1.5.1.linux-amd64.go1.15.6.tar.gz
    sudo mv protoc-gen-doc /usr/local/bin/
    
    2- sudo unzip -o protoc-X.Y.Z-linux-x86_64.zip -d /usr/local bin/protoc
    sudo unzip -o protoc-X.Y.Z-linux-x86_64.zip -d /usr/local 'include/*'
    
    NOTE: X.Y.Z = version of downloaded protoc zip
    
    3- chown X:Y /usr/local/bin/protoc
    X is the user (or owner) of the file or directory.
    Y is the group associated with the file or directory.
---
    ## TR
    --proto_path=  proto dosyalarinin konumunu belirtir
    --doc_out= kismi file export nereye yapilacak
    $(find ./togg-ms-protos/address -name "*.proto") --> burada birlestirmek istedigin dosyalarin path'i ve isim filtesi
    
    --doc_opt secenekleri;
    --doc_opt=html,index.html
    --doc_opt=docbook,docbook.xml
    --doc_opt=markdown,docs.md
    --doc_opt=json,docs.json
    
    tek bir .proto icin doc olusturmak;
    protoc --proto_path=./togg-ms-protos/ --doc_out=./togg-ms-protos/address/ --doc_opt=html,index.html ./togg-ms-protos/address/address_model.proto
    
    bir file path altindaki birden fazla .proto nun dokumanini tek bir file icinde toplamak
    protoc --proto_path=./togg-ms-protos/ $(find ./togg-ms-protos/address -name "*.proto") --doc_out=./togg-ms-protos/address/ --doc_opt=html,index.html
---
    ## EN
    --proto_path=  defines proto files path
    --doc_out= defines export path of generated file
    $(find ./togg-ms-protos/address -name "*.proto") --> defines name and path of file that wanted to merge. 
                                                    --> In this example, all files ending with .proto under the /address path are selected.
    
    --doc_opt secenekleri;
    --doc_opt=html,index.html
    --doc_opt=docbook,docbook.xml
    --doc_opt=markdown,docs.md
    --doc_opt=json,docs.json
    
    To create md doc file for just one proto file, use the following command;
    protoc --proto_path=./togg-ms-protos/ --doc_out=./togg-ms-protos/address/ --doc_opt=html,index.html ./togg-ms-protos/address/address_model.proto
    
    Collecting multiple .proto documents under a file path into a single file, use the following command;
    protoc --proto_path=./togg-ms-protos/ $(find ./togg-ms-protos/address -name "*.proto") --doc_out=./togg-ms-protos/address/ --doc_opt=html,index.html
