/*
 * COPYRIGHT 2017 SEAGATE LLC
 *
 * THIS DRAWING/DOCUMENT, ITS SPECIFICATIONS, AND THE DATA CONTAINED
 * HEREIN, ARE THE EXCLUSIVE PROPERTY OF SEAGATE TECHNOLOGY
 * LIMITED, ISSUED IN STRICT CONFIDENCE AND SHALL NOT, WITHOUT
 * THE PRIOR WRITTEN PERMISSION OF SEAGATE TECHNOLOGY LIMITED,
 * BE REPRODUCED, COPIED, OR DISCLOSED TO A THIRD PARTY, OR
 * USED FOR ANY PURPOSE WHATSOEVER, OR STORED IN A RETRIEVAL SYSTEM
 * EXCEPT AS ALLOWED BY THE TERMS OF SEAGATE LICENSES AND AGREEMENTS.
 *
 * YOU SHOULD HAVE RECEIVED A COPY OF SEAGATE'S LICENSE ALONG WITH
 * THIS RELEASE. IF NOT PLEASE CONTACT A SEAGATE REPRESENTATIVE
 * http://www.seagate.com/contact
 *
 * Original author: Sushant Mane <sushant.mane@seagate.com>
 * Original creation date: 03-Feb-2017
 */

package com.seagates3.controller;

import com.seagates3.authentication.ClientRequestParser;
import com.seagates3.authentication.ClientRequestToken;
import com.seagates3.authentication.SignatureValidator;
import com.seagates3.authorization.Authorizer;
import com.seagates3.authserver.AuthServerConfig;
import com.seagates3.authserver.IAMResourceMapper;
import com.seagates3.authserver.ResourceMap;
import com.seagates3.exception.AuthResourceNotFoundException;
import com.seagates3.exception.InternalServerException;
import com.seagates3.exception.InvalidAccessKeyException;
import com.seagates3.exception.InvalidRequestorException;
import com.seagates3.model.Account;
import com.seagates3.model.Requestor;
import com.seagates3.response.ServerResponse;
import com.seagates3.response.generator.AuthenticationResponseGenerator;
import com.seagates3.service.RequestorService;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.mockpolicies.Slf4jMockPolicy;
import org.powermock.core.classloader.annotations.MockPolicy;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.internal.WhiteboxImpl;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@MockPolicy(Slf4jMockPolicy.class)
@PowerMockIgnore({"javax.management.*"})
@PrepareForTest({IAMResourceMapper.class, IAMController.class,
        RequestorService.class, ClientRequestParser.class, Authorizer.class,
        AuthenticationResponseGenerator.class, AuthServerConfig.class})
public class IAMControllerTest {

    private IAMController controller;
    private FullHttpRequest httpRequest;
    private Map<String, String> requestBody;
    private ServerResponse serverResponse;
    private ResourceMap resourceMap;
    private Requestor requestor;

    @Before
    public void setUp() throws Exception {
        mockStatic(IAMResourceMapper.class);
        mockStatic(RequestorService.class);
        mockStatic(ClientRequestParser.class);
        mockStatic(AuthServerConfig.class);

        resourceMap = mock(ResourceMap.class);
        httpRequest = mock(FullHttpRequest.class);
        serverResponse = mock(ServerResponse.class);
        requestor = mock(Requestor.class);

        controller = new IAMController();
        requestBody = new TreeMap<>();
    }

    @Test
    public void serveTest_Action_CreateAccount() throws Exception {
        requestBody.put("Action", "CreateAccount");
        IAMController controllerSpy = spy(controller);
        when(IAMResourceMapper.getResourceMap("CreateAccount")).thenReturn(resourceMap);
        whenNew(Requestor.class).withNoArguments().thenReturn(requestor);
        doReturn(Boolean.TRUE).when(controllerSpy, "validateRequest", resourceMap, requestBody);
        doReturn(serverResponse).when(
                controllerSpy, "performAction", resourceMap, requestBody,requestor);

        ServerResponse response = controllerSpy.serve(httpRequest, requestBody);

        assertEquals(serverResponse, response);
        verifyPrivate(controllerSpy).invoke("validateRequest", resourceMap, requestBody);
        verifyPrivate(controllerSpy).invoke("performAction", resourceMap, requestBody, requestor);
    }

    @Test
    public void serveTest_InvalidAccessKeyException() throws Exception {
        requestBody.put("Action", "CreateUser");
        ClientRequestToken clientRequestToken = mock(ClientRequestToken.class);
        InvalidAccessKeyException exception = mock(InvalidAccessKeyException.class);
        when(ClientRequestParser.parse(httpRequest, requestBody)).thenReturn(clientRequestToken);
        when(exception.getServerResponse()).thenReturn(serverResponse);
        when(RequestorService.getRequestor(clientRequestToken)).thenThrow(exception);

        ServerResponse response = controller.serve(httpRequest, requestBody);

        assertEquals(serverResponse, response);
    }

    @Test
    public void serveTest_InternalServerException() throws Exception {
        requestBody.put("Action", "CreateUser");
        ClientRequestToken clientRequestToken = mock(ClientRequestToken.class);
        InternalServerException exception = mock(InternalServerException.class);
        when(ClientRequestParser.parse(httpRequest, requestBody)).thenReturn(clientRequestToken);
        when(exception.getServerResponse()).thenReturn(serverResponse);
        when(RequestorService.getRequestor(clientRequestToken)).thenThrow(exception);

        ServerResponse response = controller.serve(httpRequest, requestBody);

        assertEquals(serverResponse, response);
    }

    @Test
    public void serveTest_InvalidRequestorException() throws Exception {
        requestBody.put("Action", "CreateUser");
        ClientRequestToken clientRequestToken = mock(ClientRequestToken.class);
        InvalidRequestorException exception = mock(InvalidRequestorException.class);
        when(ClientRequestParser.parse(httpRequest, requestBody)).thenReturn(clientRequestToken);
        when(exception.getServerResponse()).thenReturn(serverResponse);
        when(RequestorService.getRequestor(clientRequestToken)).thenThrow(exception);

        ServerResponse response = controller.serve(httpRequest, requestBody);

        assertEquals(serverResponse, response);
    }

    @Test
    public void serveTest_AuthorizeUser() throws Exception {
        String expectedResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=" +
                "\"no\"?><AuthorizeUserResponse xmlns=\"https://iam.seagate.com/doc/2010-05-08/" +
                "\"><AuthorizeUserResult><UserId>MH12</UserId><UserName>tylerdurden</UserName>" +
                "<AccountId>NS5144</AccountId><AccountName>jack</AccountName></AuthorizeUser" +
                "Result><ResponseMetadata><RequestId>0000</RequestId></ResponseMetadata>" +
                "</AuthorizeUserResponse>";

        requestBody.put("Action", "AuthorizeUser");
        ClientRequestToken clientRequestToken = mock(ClientRequestToken.class);
        Account account = mock(Account.class);
        File file = mock(File.class);
        when(ClientRequestParser.parse(httpRequest, requestBody)).thenReturn(clientRequestToken);
        when(requestor.getId()).thenReturn("MH12");
        when(requestor.getName()).thenReturn("tylerdurden");
        when(requestor.getAccount()).thenReturn(account);
        when(account.getId()).thenReturn("NS5144");
        when(account.getName()).thenReturn("jack");
        when(RequestorService.getRequestor(clientRequestToken)).thenReturn(requestor);
        when(file.exists()).thenReturn(Boolean.FALSE);
        whenNew(File.class).withArguments("/tmp/seagate_s3_user_unauthorized").thenReturn(file);

        ServerResponse response = controller.serve(httpRequest, requestBody);

        assertEquals(HttpResponseStatus.OK, response.getResponseStatus());
        assertEquals(expectedResponseBody, response.getResponseBody());
    }

    @Test
    public void serveTest_Unauthorized() throws Exception {
        requestBody.put("Action", "AuthorizeUser");
        ClientRequestToken clientRequestToken = mock(ClientRequestToken.class);
        Account account = mock(Account.class);
        File file = mock(File.class);
        when(ClientRequestParser.parse(httpRequest, requestBody)).thenReturn(clientRequestToken);
        when(requestor.getId()).thenReturn("MH12");
        when(requestor.getName()).thenReturn("tylerdurden");
        when(requestor.getAccount()).thenReturn(account);
        when(account.getId()).thenReturn("NS5144");
        when(account.getName()).thenReturn("jack");
        when(RequestorService.getRequestor(clientRequestToken)).thenReturn(requestor);
        when(file.exists()).thenReturn(Boolean.TRUE);
        whenNew(File.class).withArguments("/tmp/seagate_s3_user_unauthorized").thenReturn(file);

        ServerResponse response = controller.serve(httpRequest, requestBody);

        assertEquals(HttpResponseStatus.UNAUTHORIZED, response.getResponseStatus());
        assertThat(response.getResponseBody(), containsString(
                "You are not authorized to perform this operation. Check your IAM policies," +
                        " and ensure that you are using the correct access keys."));
    }

    @Test
    public void serveTest_IncorrectSignature() throws Exception {
        requestBody.put("Action", "CreateUser");
        ClientRequestToken clientRequestToken = mock(ClientRequestToken.class);
        SignatureValidator signatureValidator = mock(SignatureValidator.class);
        when(ClientRequestParser.parse(httpRequest, requestBody)).thenReturn(clientRequestToken);
        when(RequestorService.getRequestor(clientRequestToken)).thenReturn(requestor);
        whenNew(SignatureValidator.class).withNoArguments().thenReturn(signatureValidator);
        when(signatureValidator.validate(clientRequestToken, requestor))
                .thenReturn(serverResponse);
        when(serverResponse.getResponseStatus()).thenReturn(HttpResponseStatus.UNAUTHORIZED);

        ServerResponse response = controller.serve(httpRequest, requestBody);

        assertEquals(serverResponse, response);
        assertEquals(HttpResponseStatus.UNAUTHORIZED, response.getResponseStatus());
    }

    @Test
    public void serveTest_AuthenticateUser() throws Exception {
        requestBody.put("Action", "AuthenticateUser");
        ClientRequestToken clientRequestToken = mock(ClientRequestToken.class);
        SignatureValidator signatureValidator = mock(SignatureValidator.class);
        AuthenticationResponseGenerator responseGenerator
                = mock(AuthenticationResponseGenerator.class);
        when(ClientRequestParser.parse(httpRequest, requestBody))
                .thenReturn(clientRequestToken);
        when(RequestorService.getRequestor(clientRequestToken))
                .thenReturn(requestor);
        whenNew(SignatureValidator.class).withNoArguments()
                .thenReturn(signatureValidator);
        when(signatureValidator.validate(clientRequestToken, requestor))
                .thenReturn(serverResponse);
        when(serverResponse.getResponseStatus())
                .thenReturn(HttpResponseStatus.OK);
        whenNew(AuthenticationResponseGenerator.class).withNoArguments()
                .thenReturn(responseGenerator);
        when(responseGenerator.generateAuthenticatedResponse(requestor, clientRequestToken))
                .thenReturn(serverResponse);
        when(serverResponse.getResponseBody()).thenReturn("<AuthenticateUser>");

        IAMController controller = new IAMController();
        ServerResponse response = controller.serve(httpRequest, requestBody);

        assertEquals(serverResponse, response);
        assertEquals(HttpResponseStatus.OK, response.getResponseStatus());
    }

    @Test
    public void serveTest_BadRequest() throws Exception {
        requestBody.put("Action", "CreateUser");
        when(ClientRequestParser.parse(httpRequest, requestBody)).thenReturn(null);

        ServerResponse response = controller.serve(httpRequest, requestBody);

        assertThat(response.getResponseBody(),
                containsString("Bad Request. Check request headers and body."));
        assertEquals(HttpResponseStatus.BAD_REQUEST, response.getResponseStatus());
    }

    @Test
    public void serveTest_InvalidParams() throws Exception {
        requestBody.put("Action", "ListAccounts");
        IAMController controllerSpy = spy(controller);
        when(IAMResourceMapper.getResourceMap("ListAccounts")).thenReturn(resourceMap);
        doReturn(Boolean.FALSE).when(controllerSpy, "validateRequest", resourceMap, requestBody);

        ServerResponse response = controllerSpy.serve(httpRequest, requestBody);

        assertThat(response.getResponseBody(), containsString(
                "An invalid or out-of-range value was supplied for the input parameter."));
        assertEquals(HttpResponseStatus.BAD_REQUEST, response.getResponseStatus());
        verifyPrivate(controllerSpy).invoke("validateRequest", resourceMap, requestBody);
    }

    @Test
    public void serveTest_AuthResourceNotFoundException() throws Exception {
        requestBody.put("Action", "CreateAccount");
        when(IAMResourceMapper.getResourceMap("CreateAccount"))
                .thenThrow(AuthResourceNotFoundException.class);

        ServerResponse response = controller.serve(httpRequest, requestBody);

        assertThat(response.getResponseBody(),
                containsString("The requested operation is not supported."));
        assertEquals(HttpResponseStatus.UNAUTHORIZED, response.getResponseStatus());
    }

    @Test
    public void validateRequestTest() throws Exception {
        when(resourceMap.getParamValidatorClass())
                .thenReturn("com.seagates3.parameter.validator.AccountParameterValidator");
        when(resourceMap.getParamValidatorMethod()).thenReturn("isValidCreateParams");

        Boolean result = WhiteboxImpl.invokeMethod(controller, "validateRequest",
                resourceMap, requestBody);

        assertFalse(result);
    }

    @Test
    public void validateRequestTest_ClassNotFoundException() throws Exception {
        when(resourceMap.getParamValidatorClass())
                .thenReturn("com.seagates3.parameter.validator.UnknownParameterValidator");
        when(resourceMap.getParamValidatorMethod()).thenReturn("isValidCreateParams");

        Boolean result = WhiteboxImpl.invokeMethod(controller, "validateRequest",
                resourceMap, requestBody);

        assertFalse(result);
    }

    @Test
    public void validateRequestTest_NoSuchMethodException() throws Exception {
        when(resourceMap.getParamValidatorClass())
                .thenReturn("com.seagates3.parameter.validator.AccountParameterValidator");
        when(resourceMap.getParamValidatorMethod()).thenReturn("isValidUnknownParams");

        Boolean result = WhiteboxImpl.invokeMethod(controller, "validateRequest",
                resourceMap, requestBody);

        assertFalse(result);
    }

    @Test
    public void validateRequestTest_N() throws Exception {
        when(resourceMap.getParamValidatorClass())
                .thenReturn("com.seagates3.parameter.validator.AccountParameterValidator");
        when(resourceMap.getParamValidatorMethod()).thenReturn("isValidCreateParams");

        Boolean result = WhiteboxImpl.invokeMethod(controller, "validateRequest",
                resourceMap, requestBody);

        assertFalse(result);
    }

    @Test
    public void performActionTest() throws Exception {
        when(resourceMap.getControllerClass())
                .thenReturn("com.seagates3.controller.AccountController");
        when(resourceMap.getControllerAction()).thenReturn("create");

        ServerResponse result = WhiteboxImpl.invokeMethod(controller, "performAction",
                resourceMap, requestBody, requestor);

        assertNull(result);
    }

    @Test
    public void performActionTest_ClassNotFoundException() throws Exception {
        when(resourceMap.getControllerClass())
                .thenReturn("com.seagates3.controller.UnknownController");

        ServerResponse result = WhiteboxImpl.invokeMethod(controller, "performAction",
                resourceMap, requestBody, requestor);

        assertNull(result);
    }
}