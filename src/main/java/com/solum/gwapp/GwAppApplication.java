package com.solum.gwapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.client.RestTemplate;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@SpringBootApplication
@PropertySource(value={"file:${aims.root.path}/env/gw.properties"})
public class GwAppApplication {

   public static void main(String[] args) {
      SpringApplication.run(GwAppApplication.class, args);
   }

   @Bean
   public RestTemplate getRestTemplate() {
      return new RestTemplate();
   }

   @Bean
   public Boolean disableSSLValidation() throws Exception {
      final SSLContext sslContext = SSLContext.getInstance("TLS");

      sslContext.init(null, new TrustManager[]{new X509TrustManager() {
         @Override
         public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
         }

         @Override
         public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
         }

         @Override
         public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
         }
      }}, null);

      HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
      HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
         public boolean verify(String hostname, SSLSession session) {
            return true;
         }
      });

      return true;
   }
}
