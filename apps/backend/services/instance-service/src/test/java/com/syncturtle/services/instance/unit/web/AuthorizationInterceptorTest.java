// package com.syncturtle.services.instance.unit.web;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.verifyNoInteractions;
// import static org.mockito.Mockito.when;

// import java.lang.reflect.Method;
// import java.util.UUID;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.mock.web.MockHttpServletRequest;
// import org.springframework.mock.web.MockHttpServletResponse;
// import org.springframework.web.method.HandlerMethod;

// import com.syncturtle.common.security.annotation.AllowAnonymous;
// import com.syncturtle.common.security.annotation.RequireInstancePermission;
// import com.syncturtle.common.web.context.RequestUserContext;
// import
// com.syncturtle.services.instance.configuration.web.authorization.AuthorizationInterceptor;
// import
// com.syncturtle.services.instance.configuration.web.authorization.AuthorizationResponseWriter;
// import
// com.syncturtle.services.instance.service.authz.InstanceAuthorizationService;

// import jakarta.servlet.http.HttpServletResponse;

// @ExtendWith(MockitoExtension.class)
// public class AuthorizationInterceptorTest {

// @Mock
// RequestUserContext ctx;
// @Mock
// InstanceAuthorizationService service;

// AuthorizationInterceptor interceptor;

// static class DemoController {
// @AllowAnonymous
// public void anon() {
// }

// @RequireInstancePermission(minRole = 15)
// public void admin() {
// }

// public void unannotated() {
// }
// }

// @BeforeEach
// void setup() {
// AuthorizationResponseWriter responseWriter = new
// AuthorizationResponseWriter();

// interceptor = new AuthorizationInterceptor(ctx, service, responseWriter,
// true);
// }

// @Test
// void allowAnonymous_shouldPass() throws Exception {
// // arrange
// MockHttpServletRequest request = new MockHttpServletRequest();
// MockHttpServletResponse response = new MockHttpServletResponse();
// // conditions
// // act
// boolean allowed = interceptor.preHandle(request, response, handler(new
// DemoController(), "anon"));
// // assertions
// assertThat(allowed).isTrue();
// assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
// // verify
// verifyNoInteractions(ctx, service);
// }

// @Test
// void requireAdmin_whenNotAuthenticated_sets401() throws Exception {
// // arrange
// MockHttpServletRequest request = new MockHttpServletRequest();
// MockHttpServletResponse response = new MockHttpServletResponse();
// // conditions
// when(ctx.isAuthenticated()).thenReturn(false);
// // act
// boolean allowed = interceptor.preHandle(request, response, handler(new
// DemoController(), "admin"));
// // assertions
// assertThat(allowed).isFalse();
// assertThat(response.getStatus()).isEqualTo(401);
// // verify
// verify(ctx).isAuthenticated();
// verifyNoInteractions(service);
// }

// @Test
// void requireAdmin_whenAuthenticatedButNotAdmin_sets403() throws Exception {
// // arrange
// UUID userId = UUID.randomUUID();
// MockHttpServletRequest request = new MockHttpServletRequest();
// MockHttpServletResponse response = new MockHttpServletResponse();
// // conditions
// when(ctx.isAuthenticated()).thenReturn(true);
// when(ctx.getUserId()).thenReturn(userId);
// when(service.hasInstanceRoleAtLeast(userId, 15)).thenReturn(false);
// // act
// boolean allowed = interceptor.preHandle(request, response, handler(new
// DemoController(), "admin"));
// // assertions
// assertThat(allowed).isFalse();
// assertThat(response.getStatus()).isEqualTo(403);
// // verify
// verify(service).hasInstanceRoleAtLeast(userId, 15);
// }

// private HandlerMethod handler(Object controller, String methodName) throws
// Exception {
// Method method = controller.getClass().getMethod(methodName);
// return new HandlerMethod(controller, method);
// }

// }
