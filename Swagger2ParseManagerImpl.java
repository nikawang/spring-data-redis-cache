package com.tsp.onecode.application.manager.impl;

import com.gcommon.starter.base.exception.ValidationException;
import com.gcommon.starter.lowcode.exception.ValidatorErrorCodeEnum;
import com.tsp.onecode.application.api.dto.ApiDefinitionDTO;
import com.tsp.onecode.application.api.dto.ApiParamDTO;
import com.tsp.onecode.application.api.dto.BatchImportDTO;
import com.tsp.onecode.application.manager.ApiDocParseManager;
import com.tsp.onecode.domain.shared.enums.ApiDefinitionHttpType;
import com.tsp.onecode.domain.shared.enums.ApiInputParamBodyType;
import com.tsp.onecode.domain.shared.enums.ApiParamDataType;
import com.tsp.onecode.domain.shared.enums.SwaggerParamDataType;
import io.swagger.models.*;
import io.swagger.models.parameters.*;
import io.swagger.models.properties.*;
import io.swagger.parser.Swagger20Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.ArrayList;

import java.util.List;

@Service("swagger2Manager")
public class Swagger2ParseManagerImpl implements ApiDocParseManager {

    private static final Logger logger = LoggerFactory.getLogger(Swagger2ParseManagerImpl.class);

    @Override
    public List<ApiDefinitionDTO> apiParse(BatchImportDTO apiSwaggerImportDto) {
        List<ApiDefinitionDTO> apiDefinitionDtos = new ArrayList<>();
        try {
            Swagger swagger = new Swagger20Parser().parse(apiSwaggerImportDto.getContent());
            apiDefinitionDtos = swagger2Parse(swagger, apiSwaggerImportDto);
        } catch (IOException e) {
            throw new ValidationException(ValidatorErrorCodeEnum.PARAMETER_ERROR, e, "api data parse fail!");
        }
        return apiDefinitionDtos;
    }

    /**
     * Swagger2解析
     */
    public List<ApiDefinitionDTO> swagger2Parse(Swagger swagger, BatchImportDTO apiSwaggerImportDto) {
        List<ApiDefinitionDTO> apiDefinitionVoS = new ArrayList<>();
        try {
            //所有的api信息
            swagger.getPaths().forEach((s, path) -> path.getOperationMap().forEach((httpMethod, operation) -> {
                ApiDefinitionDTO apiDefinitionDTO = new ApiDefinitionDTO();
                methodAndContentType(httpMethod.name(), apiDefinitionDTO);
                apiDefinitionDTO.setUrl(s);
                apiDefinitionDTO.setName(operation.getSummary());
                apiDefinitionDTO.setDescription(operation.getDescription());
                //入参处理
                swagger2InputParamHandle(swagger, operation,apiDefinitionDTO);
                //出参处理
                apiDefinitionDTO.setRes(swagger2OutParamHandle(swagger, operation));

                apiDefinitionVoS.add(apiDefinitionDTO);

            }));
        } catch (Exception e) {
            throw new ValidationException(ValidatorErrorCodeEnum.PARAMETER_ERROR, e, "swagger api data parse "
                    + "fail!");
        }

        return apiDefinitionVoS;
    }

    private ApiDefinitionDTO methodAndContentType(String httpMethod, ApiDefinitionDTO apiDefinitionDTO){
        //默认 application/json
        apiDefinitionDTO.setContentType(ApiInputParamBodyType.JSON.getEnumAttrName());
        if (ApiDefinitionHttpType.GET.name().equalsIgnoreCase(httpMethod)) {
            apiDefinitionDTO.setMethod(ApiDefinitionHttpType.GET);
            apiDefinitionDTO.setContentType(ApiInputParamBodyType.QUERY.getEnumAttrName());
        } else if (ApiDefinitionHttpType.POST.name().equalsIgnoreCase(httpMethod)) {
            apiDefinitionDTO.setMethod(ApiDefinitionHttpType.POST);
        } else if (ApiDefinitionHttpType.PUT.name().equalsIgnoreCase(httpMethod)) {
            apiDefinitionDTO.setMethod(ApiDefinitionHttpType.PUT);
        } else if (ApiDefinitionHttpType.DELETE.name().equalsIgnoreCase(httpMethod)) {
            apiDefinitionDTO.setMethod(ApiDefinitionHttpType.DELETE);
        }
        return apiDefinitionDTO;

    }

    /**
     * 入参处理
     */
    private  ApiDefinitionDTO swagger2InputParamHandle(Swagger swagger, Operation operation,ApiDefinitionDTO apiDefinitionDTO) {
        List<ApiParamDTO> reqParamsPathList = new ArrayList<>();
        List<ApiParamDTO> reqBodyList = new ArrayList<>();
        List<ApiParamDTO> reqParamsQueryList = new ArrayList<>();
        List<ApiParamDTO> reqHedaderList = new ArrayList<>();
        //遍历请求参数信息
        for (Parameter parameter : operation.getParameters()) {
            if (parameter instanceof PathParameter) {
                //处理PathParameter
                handlePathParameter(parameter, reqParamsPathList);
            } else if (parameter instanceof BodyParameter) {
                //处理BodyParameter
                handleBodyParameter(swagger, parameter, reqBodyList);
            } else if (parameter instanceof FormParameter) {
                //处理FormParameter
                handleFormParameter(swagger, parameter, reqBodyList);
                apiDefinitionDTO.setContentType(ApiInputParamBodyType.FORM.getEnumAttrName());
            } else if (parameter instanceof QueryParameter) {
                //处理QueryParameter
                handleQueryParameter(swagger, parameter, reqParamsQueryList);
            }else if (parameter instanceof HeaderParameter) {
                //处理HeaderParameter
                handleHeaderParameter(swagger, parameter, reqHedaderList);
            }
        }
        apiDefinitionDTO.setReqParamsPath(reqParamsPathList);
        apiDefinitionDTO.setReqBody(reqBodyList);
        apiDefinitionDTO.setReqParamsQuery(reqParamsQueryList);
        apiDefinitionDTO.setReqHedader(reqHedaderList);
        return apiDefinitionDTO;
    }

    /**
     * 处理handlePathParameter
     */
    private static void handlePathParameter(Parameter parameter, List<ApiParamDTO> inputParamVOS) {
        try {
            ApiParamDTO inputParam = new ApiParamDTO();
            inputParam.setLabel(parameter.getName());
            inputParam.setName(parameter.getDescription());
            String type = ((PathParameter) parameter).getType();
            dataTypeHandle(inputParam,type);
            inputParamVOS.add(inputParam);
        } catch (Exception e) {
            logger.warn("Swagger2解析PathParameter异常：", e);
        }
    }

    /**
     * 处理BodyParameter
     */
    private static void handleBodyParameter(Swagger swagger, Parameter parameter, List<ApiParamDTO> inputParamVOS) {
        try {
            BodyParameter bodyParameter = (BodyParameter) parameter;
            Model model = bodyParameter.getSchema();
            handleParamArrayProperty(swagger, model, inputParamVOS);
        } catch (Exception e) {
            logger.warn("Swagger2解析BodyParameter异常：", e);
        }
    }


    /**
     * 处理FormParameter
     * @param swagger
     * @param apiInputParamVOList
     * @param parameter
     */
    private static void handleFormParameter(Swagger swagger, Parameter parameter,List<ApiParamDTO> apiInputParamVOList) {
        try {
            ApiParamDTO inputParam = new ApiParamDTO();
            FormParameter formParameter = (FormParameter) parameter;
            inputParam.setLabel(formParameter.getName());
            inputParam.setName(formParameter.getDescription());
            dataTypeHandle(inputParam,formParameter.getType());
            apiInputParamVOList.add(inputParam);
        } catch (Exception e) {
            logger.warn("Swagger2解析FormParameter异常：", e);
        }
    }

    /**
     * 处理HeaderParameter
     */
    private static void handleHeaderParameter(Swagger swagger, Parameter parameter, List<ApiParamDTO> inputParamVOS) {
        HeaderParameter headerParameter = (HeaderParameter) parameter;
        ApiParamDTO inputParam = new ApiParamDTO();
        inputParam.setLabel(headerParameter.getName());
        inputParam.setName(headerParameter.getDescription());
        inputParam.setType(ApiParamDataType.TEXT);
        inputParamVOS.add(inputParam);
    }

    /**
     * 处理QueryParameter
     */
    private static void handleQueryParameter(Swagger swagger, Parameter parameter,List<ApiParamDTO> inputParamVOS) {
        try {
            ApiParamDTO inputParam = new ApiParamDTO();
            inputParam.setName(parameter.getDescription());
            inputParam.setLabel(parameter.getName());
            String type = ((QueryParameter) parameter).getType();
            dataTypeHandle(inputParam,type);
            inputParamVOS.add(inputParam);
        } catch (Exception e) {
            logger.warn("Swagger2解析QueryParameter异常：", e);
        }
    }

    /**
     * 出参处理
     */
    private static List<ApiParamDTO> swagger2OutParamHandle(Swagger swagger, Operation operation) {
        List<ApiParamDTO> apiOutParams = new ArrayList<>();
        try {
            Response response = operation.getResponses().get("200");
            if (response == null) {
                return apiOutParams;
            }
            Model model = response.getResponseSchema();
            handleParamArrayProperty(swagger, model, apiOutParams);
        } catch (Exception e) {
            logger.warn("Swagger2解析出参异常：", e);
        }
        return apiOutParams;
    }

    private static void handleParamArrayProperty(Swagger swagger,  Model model, List<ApiParamDTO> apiParams){
        if (model != null && model.getProperties() != null) {
            model.getProperties().forEach((s, property) -> {
                ApiParamDTO apiOutParam = new ApiParamDTO();
                apiOutParam.setLabel(s);
                apiOutParam.setName(property.getDescription());
                dataTypeHandle(property,apiOutParam);
                if (property instanceof ArrayProperty) {
                    //处理数组
                    handleParamArrayProperty(swagger, property, apiOutParam);
                } else if (property instanceof ObjectProperty) {
                    apiOutParam.setType(ApiParamDataType.OBJECT);
                    //处理对象
                    handleParamObjectModel(swagger,apiOutParam, (ObjectProperty)property);
                }
                apiParams.add(apiOutParam);
            });
        }
    }

    /**
     * 处理出参数组
     */
    private static void handleParamArrayProperty(Swagger swagger, Property property, ApiParamDTO apiParamDTO) {
        try {
            ArrayProperty arrayProperty = (ArrayProperty) property;
            if(arrayProperty.getItems() instanceof ObjectProperty) {
                handleParamObjectModel(swagger, apiParamDTO, (ObjectProperty) arrayProperty.getItems());
            }
        } catch (Exception e) {
            logger.warn("Swagger2解析出参ArrayProperty异常：", e);
        }
    }

    private static void handleParamObjectModel(Swagger swagger, ApiParamDTO outParam, ObjectProperty objectProperty) {
        if (objectProperty != null && objectProperty.getProperties() != null) {
            List<ApiParamDTO> childrenList = new ArrayList<>();
            objectProperty.getProperties().forEach((s, property) -> {
                ApiParamDTO children = new ApiParamDTO();
                children.setLabel(s);
                children.setName(property.getDescription());
                dataTypeHandle(property,children);
                if (property instanceof ArrayProperty) {
                    //处理出参数组
                    handleParamArrayProperty(swagger, property, children);
                } else if (property instanceof ObjectProperty) {
                    //处理出参对象
                    handleParamObjectModel(swagger,children,(ObjectProperty) property);
                }
                childrenList.add(children);
            });
            outParam.setChildren(childrenList);
        }
    }

    private static void dataTypeHandle(Property property, ApiParamDTO inputParam) {
        if (property instanceof ArrayProperty) {
            inputParam.setItemIsArray(true);
            inputParam.setType(ApiParamDataType.OBJECT);
            String dateType = ((ArrayProperty) property).getItems().getType();
            if (SwaggerParamDataType.INTEGER.getEnumAttrCode().equalsIgnoreCase(dateType)) {
                inputParam.setType(ApiParamDataType.NUMBER);
            } else if (SwaggerParamDataType.STRING.getEnumAttrCode().equalsIgnoreCase(dateType)) {
                inputParam.setType(ApiParamDataType.TEXT);
            }
        } else if (property instanceof BooleanProperty) {
            inputParam.setType(ApiParamDataType.BOOL);
        } else if (property instanceof DateProperty || property instanceof DateTimeProperty) {
            inputParam.setType(ApiParamDataType.DATE);
        } else if (property instanceof DoubleProperty || property instanceof IntegerProperty) {
            inputParam.setType(ApiParamDataType.NUMBER);
        } else if (property instanceof FloatProperty) {
            inputParam.setType(ApiParamDataType.FLOAT);
        } else if (property instanceof LongProperty) {
            inputParam.setType(ApiParamDataType.LONG);
        } else if (property instanceof StringProperty) {
            inputParam.setType(ApiParamDataType.TEXT);
        } else if (property instanceof DecimalProperty) {
            inputParam.setType(ApiParamDataType.CURRENCY);
        } else if (property instanceof ObjectProperty) {
            inputParam.setType(ApiParamDataType.OBJECT);
        }
    }

    private static void dataTypeHandle(ApiParamDTO inputParam, String  dataType) {
        inputParam.setType(ApiParamDataType.TEXT);
        if (SwaggerParamDataType.STRING.getEnumAttrCode().equalsIgnoreCase(dataType)) {
            inputParam.setType(ApiParamDataType.TEXT);
        } else if (SwaggerParamDataType.NUMBER.getEnumAttrCode().equalsIgnoreCase(dataType)
                || SwaggerParamDataType.INTEGER.getEnumAttrCode().equalsIgnoreCase(dataType)
                || SwaggerParamDataType.DOUBLE.getEnumAttrCode().equalsIgnoreCase(dataType)) {
            inputParam.setType(ApiParamDataType.NUMBER);
        } else if (SwaggerParamDataType.ARRAY.getEnumAttrCode().equalsIgnoreCase(dataType) || SwaggerParamDataType.ARRAYS.getEnumAttrCode().equalsIgnoreCase(dataType)) {
            inputParam.setItemIsArray(true);
            inputParam.setType(ApiParamDataType.OBJECT);
        } else if (SwaggerParamDataType.DATE.getEnumAttrCode().equalsIgnoreCase(dataType) || SwaggerParamDataType.TIMESTAMP.getEnumAttrCode().equalsIgnoreCase(dataType)) {
            inputParam.setType(ApiParamDataType.DATE);
        } else if (SwaggerParamDataType.BOOLEAN.getEnumAttrCode().equalsIgnoreCase(dataType)) {
            inputParam.setType(ApiParamDataType.BOOL);
        } else if (SwaggerParamDataType.FILE.getEnumAttrCode().equalsIgnoreCase(dataType)) {
            inputParam.setType(ApiParamDataType.FILE);
        } else if (SwaggerParamDataType.OBJECT.getEnumAttrCode().equalsIgnoreCase(dataType)) {
            inputParam.setType(ApiParamDataType.OBJECT);
        }
    }

}
