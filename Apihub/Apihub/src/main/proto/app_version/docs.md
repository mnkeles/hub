# Protocol Documentation
<a name="top"></a>

## Table of Contents

- [app_version/version_service.proto](#app_version_version_service-proto)
    - [AddVersionRequest](#tr-com-togg-tdp-tma-ms-settings-AddVersionRequest)
    - [AddVersionResponse](#tr-com-togg-tdp-tma-ms-settings-AddVersionResponse)
    - [GetLatestVersionRequest](#tr-com-togg-tdp-tma-ms-settings-GetLatestVersionRequest)
    - [GetLatestVersionResponse](#tr-com-togg-tdp-tma-ms-settings-GetLatestVersionResponse)
    - [ListAllVersionsResponse](#tr-com-togg-tdp-tma-ms-settings-ListAllVersionsResponse)
    - [RemoveVersionRequest](#tr-com-togg-tdp-tma-ms-settings-RemoveVersionRequest)
    - [RemoveVersionResponse](#tr-com-togg-tdp-tma-ms-settings-RemoveVersionResponse)
    - [UpdateVersionRequest](#tr-com-togg-tdp-tma-ms-settings-UpdateVersionRequest)
    - [UpdateVersionResponse](#tr-com-togg-tdp-tma-ms-settings-UpdateVersionResponse)
    - [Version](#tr-com-togg-tdp-tma-ms-settings-Version)
  
    - [VersionsService](#tr-com-togg-tdp-tma-ms-settings-VersionsService)
  
- [Scalar Value Types](#scalar-value-types)



<a name="app_version_version_service-proto"></a>
<p align="right"><a href="#top">Top</a></p>

## app_version/version_service.proto



<a name="tr-com-togg-tdp-tma-ms-settings-AddVersionRequest"></a>

### AddVersionRequest



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| version | [Version](#tr-com-togg-tdp-tma-ms-settings-Version) |  | Unique id of AddVersionRequest |






<a name="tr-com-togg-tdp-tma-ms-settings-AddVersionResponse"></a>

### AddVersionResponse



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| id | [string](#string) |  | Unique id of AddVersionResponse |






<a name="tr-com-togg-tdp-tma-ms-settings-GetLatestVersionRequest"></a>

### GetLatestVersionRequest



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| platformNames | [string](#string) | repeated | The bytes for platformName |






<a name="tr-com-togg-tdp-tma-ms-settings-GetLatestVersionResponse"></a>

### GetLatestVersionResponse



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| version | [Version](#tr-com-togg-tdp-tma-ms-settings-Version) | repeated | The bytes for version |






<a name="tr-com-togg-tdp-tma-ms-settings-ListAllVersionsResponse"></a>

### ListAllVersionsResponse



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| versions | [Version](#tr-com-togg-tdp-tma-ms-settings-Version) | repeated | Unique id of ListAllVersionsResponse |






<a name="tr-com-togg-tdp-tma-ms-settings-RemoveVersionRequest"></a>

### RemoveVersionRequest



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| id | [string](#string) |  | Unique id of RemoveVersionRequest |






<a name="tr-com-togg-tdp-tma-ms-settings-RemoveVersionResponse"></a>

### RemoveVersionResponse



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| version | [Version](#tr-com-togg-tdp-tma-ms-settings-Version) |  | Unique id of RemoveVersionResponse |






<a name="tr-com-togg-tdp-tma-ms-settings-UpdateVersionRequest"></a>

### UpdateVersionRequest



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| version | [Version](#tr-com-togg-tdp-tma-ms-settings-Version) |  | Unique id of UpdateVersionRequest |






<a name="tr-com-togg-tdp-tma-ms-settings-UpdateVersionResponse"></a>

### UpdateVersionResponse



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| version | [Version](#tr-com-togg-tdp-tma-ms-settings-Version) |  | Unique id of UpdateVersionResponse |






<a name="tr-com-togg-tdp-tma-ms-settings-Version"></a>

### Version



| Field | Type | Label | Description |
| ----- | ---- | ----- | ----------- |
| id | [string](#string) |  | Unique id of version |
| platformName | [string](#string) |  | The bytes for platform name |
| version | [string](#string) |  | The bytes for version |
| buildNumber | [string](#string) |  | Build number in the version repository |





 

 

 


<a name="tr-com-togg-tdp-tma-ms-settings-VersionsService"></a>

### VersionsService
VersionsService is the service of getting the latest version, adding new versions,
deleting versions, updating versions and listing all available versions

| Method Name | Request Type | Response Type | Description |
| ----------- | ------------ | ------------- | ------------|
| GetLatestVersion | [GetLatestVersionRequest](#tr-com-togg-tdp-tma-ms-settings-GetLatestVersionRequest) | [GetLatestVersionResponse](#tr-com-togg-tdp-tma-ms-settings-GetLatestVersionResponse) | Gets the latest version |
| AddVersion | [AddVersionRequest](#tr-com-togg-tdp-tma-ms-settings-AddVersionRequest) | [AddVersionResponse](#tr-com-togg-tdp-tma-ms-settings-AddVersionResponse) | Adds a version |
| RemoveVersion | [RemoveVersionRequest](#tr-com-togg-tdp-tma-ms-settings-RemoveVersionRequest) | [RemoveVersionResponse](#tr-com-togg-tdp-tma-ms-settings-RemoveVersionResponse) | Removes a version |
| UpdateVersion | [UpdateVersionRequest](#tr-com-togg-tdp-tma-ms-settings-UpdateVersionRequest) | [UpdateVersionResponse](#tr-com-togg-tdp-tma-ms-settings-UpdateVersionResponse) | Updates an existing version |
| ListAllVersions | [.google.protobuf.Empty](#google-protobuf-Empty) | [ListAllVersionsResponse](#tr-com-togg-tdp-tma-ms-settings-ListAllVersionsResponse) | Lists all available versions for a specific platform |

 



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

