# Protocol Documentation
<a name="top"></a>

## Table of Contents

- [customer/customer_model.proto](#customer_customer_model-proto)
    - [Client](#tr-com-togg-tdp-tma-ms-customer-Client)
    - [ClientAddress](#tr-com-togg-tdp-tma-ms-customer-ClientAddress)
    - [ClientCity](#tr-com-togg-tdp-tma-ms-customer-ClientCity)
    - [ClientCountry](#tr-com-togg-tdp-tma-ms-customer-ClientCountry)
    - [ClientNationalId](#tr-com-togg-tdp-tma-ms-customer-ClientNationalId)
    - [ClientPhoneNumber](#tr-com-togg-tdp-tma-ms-customer-ClientPhoneNumber)
    - [DeactivateUserAccountRequest](#tr-com-togg-tdp-tma-ms-customer-DeactivateUserAccountRequest)
    - [GetUserInformationRequest](#tr-com-togg-tdp-tma-ms-customer-GetUserInformationRequest)
    - [GetUserInformationResponse](#tr-com-togg-tdp-tma-ms-customer-GetUserInformationResponse)
  
    - [ClientPhoneNumberVerificationStatus](#tr-com-togg-tdp-tma-ms-customer-ClientPhoneNumberVerificationStatus)
  
- [customer/customer_service.proto](#customer_customer_service-proto)
    - [CustomerService](#tr-com-togg-tdp-tma-ms-customer-client-CustomerService)
  
- [Scalar Value Types](#scalar-value-types)



<a name="customer_customer_model-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## customer/customer_model.proto



<a name="tr-com-togg-tdp-tma-ms-customer-Client"></a>

### Client
Values of client


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| natId | [ClientNationalId](#tr-com-togg-tdp-tma-ms-customer-ClientNationalId) |  | Client national id |
| first_name | [string](#string) |  | First name |
| last_name | [string](#string) |  | Last name |
| phone_number | [ClientPhoneNumber](#tr-com-togg-tdp-tma-ms-customer-ClientPhoneNumber) |  | Phone number |
| address | [ClientAddress](#tr-com-togg-tdp-tma-ms-customer-ClientAddress) |  | Address |
| email | [string](#string) |  | Email |
| language | [tr.com.togg.tdp.common.enums.UserLanguage](#tr-com-togg-tdp-common-enums-UserLanguage) |  | Language |
| birthdate | [tr.com.togg.tdp.common.model.CommonDate](#tr-com-togg-tdp-common-model-CommonDate) |  | Birthdate |
| is_adult | [google.protobuf.BoolValue](#google-protobuf-BoolValue) |  | Checks over 18 |






<a name="tr-com-togg-tdp-tma-ms-customer-ClientAddress"></a>

### ClientAddress
Values of client address


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| city | [ClientCity](#tr-com-togg-tdp-tma-ms-customer-ClientCity) |  | City |
| country | [ClientCountry](#tr-com-togg-tdp-tma-ms-customer-ClientCountry) |  | Country |
| address | [string](#string) |  | Address |






<a name="tr-com-togg-tdp-tma-ms-customer-ClientCity"></a>

### ClientCity
Values of client city


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| id | [int64](#int64) |  | City id |
| code | [string](#string) |  | City code |
| name | [string](#string) |  | City name |






<a name="tr-com-togg-tdp-tma-ms-customer-ClientCountry"></a>

### ClientCountry
Values of client country


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| id | [int64](#int64) |  | Country id |
| code | [string](#string) |  | Contry code |
| name | [string](#string) |  | Country name |






<a name="tr-com-togg-tdp-tma-ms-customer-ClientNationalId"></a>

### ClientNationalId
Values of client national id


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| country | [ClientCountry](#tr-com-togg-tdp-tma-ms-customer-ClientCountry) |  | Required |
| national_id_number | [string](#string) |  | Required |






<a name="tr-com-togg-tdp-tma-ms-customer-ClientPhoneNumber"></a>

### ClientPhoneNumber
Values of client phone number


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| country_code | [int32](#int32) |  | Client country code |
| national_number | [uint64](#uint64) |  | Client national number |
| verification_status | [ClientPhoneNumberVerificationStatus](#tr-com-togg-tdp-tma-ms-customer-ClientPhoneNumberVerificationStatus) |  | Client phone number verification status

read-only |






<a name="tr-com-togg-tdp-tma-ms-customer-DeactivateUserAccountRequest"></a>

### DeactivateUserAccountRequest
Deactivates an already active user.


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| header | [tr.com.togg.tdp.common.model.RequestHeader](#tr-com-togg-tdp-common-model-RequestHeader) |  | The value of header |






<a name="tr-com-togg-tdp-tma-ms-customer-GetUserInformationRequest"></a>

### GetUserInformationRequest
Used to get user information


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| header | [tr.com.togg.tdp.common.model.RequestHeader](#tr-com-togg-tdp-common-model-RequestHeader) |  | The value of header |
| offer_id | [int64](#int64) |  | The value of offer id |






<a name="tr-com-togg-tdp-tma-ms-customer-GetUserInformationResponse"></a>

### GetUserInformationResponse
The result is the GetUserInformationRequest


| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| userInformation | [Client](#tr-com-togg-tdp-tma-ms-customer-Client) |  | Used to get user information values |
| billing_address | [ClientAddress](#tr-com-togg-tdp-tma-ms-customer-ClientAddress) |  | The value of billing address |





 


<a name="tr-com-togg-tdp-tma-ms-customer-ClientPhoneNumberVerificationStatus"></a>

### ClientPhoneNumberVerificationStatus
Values of client phone number verification status

| Name | Number | Description |
| ---- | ------ | ----------- |
| VERIFICATION_STATUS_UNSPECIFIED | 0 | Verification status unspecified |
| UNVERIFIED | 1 | Unverified |
| VERIFIED | 2 | Verified |


 

 

 



<a name="customer_customer_service-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## customer/customer_service.proto


 

 

 


<a name="tr-com-togg-tdp-tma-ms-customer-client-CustomerService"></a>

### CustomerService
The service where deactivates an already active user account and get user information

| Method Name | Request Type | Response Type | Description |
| ----------- | ------------ | ------------- | ------------|
| DeactivateUserAccount | [.tr.com.togg.tdp.tma.ms.customer.DeactivateUserAccountRequest](#tr-com-togg-tdp-tma-ms-customer-DeactivateUserAccountRequest) | [.google.protobuf.Empty](#google-protobuf-Empty) | Deactivates an already active user account. |
| GetUserInformation | [.tr.com.togg.tdp.tma.ms.customer.GetUserInformationRequest](#tr-com-togg-tdp-tma-ms-customer-GetUserInformationRequest) | [.tr.com.togg.tdp.tma.ms.customer.GetUserInformationResponse](#tr-com-togg-tdp-tma-ms-customer-GetUserInformationResponse) | Used to get user information |

 



## Scalar Value Types

| .proto Type | Notes | C++ | Java | Python | Go | C# | PHP | Ruby |
| ----------- | ----- | --- | ---- | ------ | -- | -- | --- | ---- |
| <a name="double" /> double |  | double | double | float | float64 | double | float | Float |
| <a name="float" /> float |  | float | float | float | float32 | float | float | Float |
| <a name="int32" /> int32 | Uses variable-length encoding. Inefficient for encoding negative numbers – if your field is likely to have negative values, use sint32 instead. | int32 | int | int | int32 | int | integer | Bignum or Fixnum (as required) |
| <a name="int64" /> int64 | Uses variable-length encoding. Inefficient for encoding negative numbers – if your field is likely to have negative values, use sint64 instead. | int64 | long | int/long | int64 | long | integer/string | Bignum |
| <a name="uint32" /> uint32 | Uses variable-length encoding. | uint32 | int | int/long | uint32 | uint | integer | Bignum or Fixnum (as required) |
| <a name="uint64" /> uint64 | Uses variable-length encoding. | uint64 | long | int/long | uint64 | ulong | integer/string | Bignum or Fixnum (as required) |
| <a name="sint32" /> sint32 | Uses variable-length encoding. Signed int value. These more efficiently encode negative numbers than regular int32s. | int32 | int | int | int32 | int | integer | Bignum or Fixnum (as required) |
| <a name="sint64" /> sint64 | Uses variable-length encoding. Signed int value. These more efficiently encode negative numbers than regular int64s. | int64 | long | int/long | int64 | long | integer/string | Bignum |
| <a name="fixed32" /> fixed32 | Always four bytes. More efficient than uint32 if values are often greater than 2^28. | uint32 | int | int | uint32 | uint | integer | Bignum or Fixnum (as required) |
| <a name="fixed64" /> fixed64 | Always eight bytes. More efficient than uint64 if values are often greater than 2^56. | uint64 | long | int/long | uint64 | ulong | integer/string | Bignum |
| <a name="sfixed32" /> sfixed32 | Always four bytes. | int32 | int | int | int32 | int | integer | Bignum or Fixnum (as required) |
| <a name="sfixed64" /> sfixed64 | Always eight bytes. | int64 | long | int/long | int64 | long | integer/string | Bignum |
| <a name="bool" /> bool |  | bool | boolean | boolean | bool | bool | boolean | TrueClass/FalseClass |
| <a name="string" /> string | A string must always contain UTF-8 encoded or 7-bit ASCII text. | string | String | str/unicode | string | string | string | String (UTF-8) |
| <a name="bytes" /> bytes | May contain any arbitrary sequence of bytes. | string | ByteString | str | []byte | ByteString | string | String (ASCII-8BIT) |

