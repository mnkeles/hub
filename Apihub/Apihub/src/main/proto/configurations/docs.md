# Protocol Documentation
<a name="top"></a>

## Table of Contents

- [configurations/configurations_admin.proto](#configurations_configurations_admin-proto)
    - [CreateConfigurationRequest](#togg-tdp-tma-ms-configurations-admin-CreateConfigurationRequest)
    - [CreateConfigurationResponse](#togg-tdp-tma-ms-configurations-admin-CreateConfigurationResponse)
    - [DeleteConfigurationRequest](#togg-tdp-tma-ms-configurations-admin-DeleteConfigurationRequest)
    - [UpdateConfigurationRequest](#togg-tdp-tma-ms-configurations-admin-UpdateConfigurationRequest)
    - [UpdateConfigurationResponse](#togg-tdp-tma-ms-configurations-admin-UpdateConfigurationResponse)
  
    - [ConfigurationsAdminService](#togg-tdp-tma-ms-configurations-admin-ConfigurationsAdminService)
  
- [configurations/configurations_model.proto](#configurations_configurations_model-proto)
    - [Configuration](#togg-tdp-tma-ms-configurations-model-Configuration)
  
- [configurations/configurations_service.proto](#configurations_configurations_service-proto)
    - [GetConfigurationRequest](#togg-tdp-tma-ms-configurations-GetConfigurationRequest)
    - [GetConfigurationResponse](#togg-tdp-tma-ms-configurations-GetConfigurationResponse)
    - [ListConfigurationsResponse](#togg-tdp-tma-ms-configurations-ListConfigurationsResponse)
    - [ListenConfigurationsRequest](#togg-tdp-tma-ms-configurations-ListenConfigurationsRequest)
    - [ListenConfigurationsResponse](#togg-tdp-tma-ms-configurations-ListenConfigurationsResponse)
  
    - [ConfigurationsService](#togg-tdp-tma-ms-configurations-ConfigurationsService)
  
- [Scalar Value Types](#scalar-value-types)



<a name="configurations_configurations_admin-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## configurations/configurations_admin.proto



<a name="togg-tdp-tma-ms-configurations-admin-CreateConfigurationRequest"></a>

### CreateConfigurationRequest



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| configuration | [togg.tdp.tma.ms.configurations.model.Configuration](#togg-tdp-tma-ms-configurations-model-Configuration) |  | The values in the configuration are taken. |






<a name="togg-tdp-tma-ms-configurations-admin-CreateConfigurationResponse"></a>

### CreateConfigurationResponse



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| configuration | [togg.tdp.tma.ms.configurations.model.Configuration](#togg-tdp-tma-ms-configurations-model-Configuration) |  | The values in the configuration are taken. |






<a name="togg-tdp-tma-ms-configurations-admin-DeleteConfigurationRequest"></a>

### DeleteConfigurationRequest



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [string](#string) |  | The value of key |






<a name="togg-tdp-tma-ms-configurations-admin-UpdateConfigurationRequest"></a>

### UpdateConfigurationRequest



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| configuration | [togg.tdp.tma.ms.configurations.model.Configuration](#togg-tdp-tma-ms-configurations-model-Configuration) |  | The values in the configuration are taken. |






<a name="togg-tdp-tma-ms-configurations-admin-UpdateConfigurationResponse"></a>

### UpdateConfigurationResponse



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| configuration | [togg.tdp.tma.ms.configurations.model.Configuration](#togg-tdp-tma-ms-configurations-model-Configuration) |  | The values in the configuration are taken. |





 

 

 


<a name="togg-tdp-tma-ms-configurations-admin-ConfigurationsAdminService"></a>

### ConfigurationsAdminService
The service used by admins where configurations are updated, deleted and created

| Method Name | Request Type | Response Type | Description |
| ----------- | ------------ | ------------- | ------------|
| UpdateConfiguration | [UpdateConfigurationRequest](#togg-tdp-tma-ms-configurations-admin-UpdateConfigurationRequest) | [UpdateConfigurationResponse](#togg-tdp-tma-ms-configurations-admin-UpdateConfigurationResponse) | Updates configurations |
| DeleteConfiguration | [DeleteConfigurationRequest](#togg-tdp-tma-ms-configurations-admin-DeleteConfigurationRequest) | [.google.protobuf.Empty](#google-protobuf-Empty) | Deletes configurations |
| CreateConfiguration | [CreateConfigurationRequest](#togg-tdp-tma-ms-configurations-admin-CreateConfigurationRequest) | [CreateConfigurationResponse](#togg-tdp-tma-ms-configurations-admin-CreateConfigurationResponse) | Creates configurations |

 



<a name="configurations_configurations_model-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## configurations/configurations_model.proto



<a name="togg-tdp-tma-ms-configurations-model-Configuration"></a>

### Configuration



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [string](#string) |  | Value of key |
| value | [google.protobuf.Value](#google-protobuf-Value) |  | Represents a double value |





 

 

 

 



<a name="configurations_configurations_service-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## configurations/configurations_service.proto



<a name="togg-tdp-tma-ms-configurations-GetConfigurationRequest"></a>

### GetConfigurationRequest



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| key | [string](#string) |  | The value of key |






<a name="togg-tdp-tma-ms-configurations-GetConfigurationResponse"></a>

### GetConfigurationResponse



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| configuration | [model.Configuration](#togg-tdp-tma-ms-configurations-model-Configuration) |  | The values in the configuration are taken |






<a name="togg-tdp-tma-ms-configurations-ListConfigurationsResponse"></a>

### ListConfigurationsResponse



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| configurations | [model.Configuration](#togg-tdp-tma-ms-configurations-model-Configuration) | repeated | The values in the configuration are taken |






<a name="togg-tdp-tma-ms-configurations-ListenConfigurationsRequest"></a>

### ListenConfigurationsRequest



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| keys | [string](#string) | repeated | The value of keys |






<a name="togg-tdp-tma-ms-configurations-ListenConfigurationsResponse"></a>

### ListenConfigurationsResponse



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| configurations | [model.Configuration](#togg-tdp-tma-ms-configurations-model-Configuration) |  | The values in the configuration are taken |





 

 

 


<a name="togg-tdp-tma-ms-configurations-ConfigurationsService"></a>

### ConfigurationsService
The service where configurations are list, get and listen

| Method Name | Request Type | Response Type | Description |
| ----------- | ------------ | ------------- | ------------|
| ListConfigurations | [.google.protobuf.Empty](#google-protobuf-Empty) | [ListConfigurationsResponse](#togg-tdp-tma-ms-configurations-ListConfigurationsResponse) | Lists configurations |
| GetConfiguration | [GetConfigurationRequest](#togg-tdp-tma-ms-configurations-GetConfigurationRequest) | [GetConfigurationResponse](#togg-tdp-tma-ms-configurations-GetConfigurationResponse) | Gets configuration |
| ListenConfigurations | [ListenConfigurationsRequest](#togg-tdp-tma-ms-configurations-ListenConfigurationsRequest) | [ListenConfigurationsResponse](#togg-tdp-tma-ms-configurations-ListenConfigurationsResponse) stream | Listen configuration |

 



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

