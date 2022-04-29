package net.unicon.lti.service.lti.test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import net.unicon.lti.service.lti.LTIDataService;
import net.unicon.lti.service.lti.LTIJWTService;
import net.unicon.lti.service.lti.impl.LTIJWTServiceImpl;
import net.unicon.lti.utils.lti.LTI3Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

public class LTIJWTServiceTest {

    @InjectMocks
    LTIJWTService ltijwtService = new LTIJWTServiceImpl();

    @Mock
    LTIDataService ltiDataService;

    @Mock
    LTI3Request lti3Request;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGenerateDLJWT() {
        try {
            when(lti3Request.getIss()).thenReturn("iss-1");
            when(lti3Request.getAud()).thenReturn("client-id-1");
            when(lti3Request.getLtiDeploymentId()).thenReturn("deployment-id-1");
            when(lti3Request.getLtiContextId()).thenReturn("context-id-1");
            when(lti3Request.getSub()).thenReturn("sub-1");
            when(ltiDataService.getLtiReactUiUrl()).thenReturn("reactui.com");

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(1024);
            KeyPair kp = kpg.generateKeyPair();
            Base64.Encoder encoder = Base64.getEncoder();
            String privateKey = "-----BEGIN PRIVATE KEY-----\n" + encoder.encodeToString(kp.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----\n";
            when(ltiDataService.getOwnPrivateKey()).thenReturn(privateKey);


            String dlJwt = ltijwtService.generateDLInitializationJWT(lti3Request);

            Mockito.verify(ltiDataService).getOwnPrivateKey();
            Mockito.verify(ltiDataService).getLtiReactUiUrl();
            Mockito.verify(lti3Request).getIss();
            Mockito.verify(lti3Request).getAud();
            Mockito.verify(lti3Request).getLtiDeploymentId();
            Mockito.verify(lti3Request).getLtiContextId();
            Mockito.verify(lti3Request).getSub();

            // validate that final jwt was signed by middleware
            Jws<Claims> finalClaims = Jwts.parser().setSigningKey(kp.getPublic()).parseClaimsJws(dlJwt);
            Claims dlClaims = finalClaims.getBody();
            assertNotNull(finalClaims);
            assertEquals("Goldilocks", dlClaims.getSubject());
            assertEquals(ltiDataService.getLtiReactUiUrl(), dlClaims.getAudience());
            assertEquals(lti3Request.getIss(), dlClaims.get("iss"));
            assertEquals(lti3Request.getAud(), dlClaims.get("client_id"));
            assertEquals(lti3Request.getLtiDeploymentId(), dlClaims.get("deployment_id"));
            assertEquals(lti3Request.getLtiContextId(), dlClaims.get("context_id"));
            assertEquals(lti3Request.getSub(), dlClaims.get("user_id"));
        } catch (GeneralSecurityException e) {
            fail("No exception should be thrown.");
        }
    }
}
