// General Page Request and Filter Types
export interface GeneralFilter {
    criteria: string;
    value?: string;
    numberValue?: number;
}

export interface GeneralPageRequest {
    offset: number;
    limit: number;
    filterList: GeneralFilter[];
}

export interface PagedResponse<T> {
    data: T[];
    totalCount: number;
    offset: number;
    limit: number;
}

// Project Types
export interface ProjectDto {
    projectId: number | null;
    name: string;
    description: string;
    shortCode: string;
    generalWebSystemDtoList?: GeneralWebSystemDto[];
}

// General Web System Types
export interface GeneralWebSystemDto {
    gnlWebSysId: number | null;
    name: string;
    shortCode: string;
    url: string;
    actv: boolean;
    projectId: number;
    isTokenUrl: boolean;
    baseUrlShortCode: string;
}

// Process Flow Types
export interface ProcessFlowDto {
    processFlowId: number | null;
    shortCode: string;
    isActive: string;
    systemShortCode?: string;
    systemShortCodeOAB?: string;
    projectId: number;
    processFlowStepList?: ProcessFlowStepDto[];
}

// API Information Types
export interface ApiInformationDto {
    id?: number;
    gnlApiInformationId?: number;
    providerSystemId?: number;
    name: string;
    shortCode: string;
    srvcName: string;                    // Service endpoint path
    headerParameters?: string;           // Semicolon-separated header names
    statusCode?: number;
    plIn?: string;                       // Request payload template
    httpMethod: string;                  // GET, POST, PUT, DELETE, etc.
    cdate?: string;                      // Creation date (ISO format)
    udate?: string;                      // Update date (ISO format)
    active?: string;                     // "Aktif" or "Pasif"
    apiShortCode?: string;
    mediaType?: string;                  // e.g., "application/json"
    headerVal?: string;
    providerSystemInfo?: {
        id: number;
        name: string;
    };
    projectId?: number;
    grpc?: boolean;                      // Is gRPC API
    serviceName?: string;                // For gRPC
    methodName?: string;                 // For gRPC
    isTokenApi?: boolean;                // Is authentication API
    externalApi?: boolean;               // Is external API
    sqlQuery?:boolean;
    // Legacy fields for backward compatibility
    url?: string;
    method?: string;
    description?: string;
}

// Process Flow Step Types
export interface ProcessFlowStepDto {
    processFlowStepId: number | null;
    gnlApiInformationId: number;
    processFlowId: number;
    stepOrder: number;
    stepShortCode: string;
    apiInformation?: ApiInformationDto;
    processFlowStepRelationList?: ProcessFlowStepRelationDto[];
    plIn?: string;
    headerExtractor?: string;
    parameterExtractor?: string;
    processFlowStepParmList?: ProcessFlowStepParmDto[];
    preHeader?: string;
}

// Process Flow Step Parameter Types
export interface ProcessFlowStepParmDto {
    processFlowStepParmId: number | null;
    shortCode: string;
    valExpression?: string;
    value?: string;
    paramOrder: number;
    useContext: boolean;
    sql?: string;
    code?: string;
}

// Process Flow Step Relation Types
export interface ProcessFlowStepRelationDto {
    processFlowStepRelationId: number | null;
    processFlowStepId: number;
    processFlowStepParmId: number | null;
    projectId: number | null;
    processFlowStep?: ProcessFlowStepDto;
    processFlowStepParameters?: ProcessFlowStepParmDto;
    processFlowStepParms?: ProcessFlowStepParmDto[];
}

// API Call Execution Types
export interface ParameterRequestDto {
    parameterContext?: Record<string, unknown>;
    globalHeaders?: Record<string, string>;
    combination?: boolean;
}

export interface ApiCallResponse {
    [stepShortCode: string]: string;
}

// Result Type
export interface Result<T = unknown> {
    success: boolean;
    message: string;
    data?: T;
}

// Chat Types
export interface ChatMessage {
    role: 'user' | 'assistant' | 'system';
    content: string;
    reasoning?: string | null;
    promptTokens?: number | null;
    completionTokens?: number | null;
    totalTokens?: number | null;
}

export interface ChatRequest {
    message: string;
    projectShortCode?: string;
    systemShortCode?: string;
    analysisReferenceId?: string;
    context?: unknown;
}

export interface ChatResponse {
    response: string;
    history: ChatMessage[] | null;
}

// Database/Environment Types
export interface DatabaseDto {
    databaseId: number | null;
    shortCode: string;
    name: string;
    url: string;
    isActive: boolean;
    projectId: number;
}
