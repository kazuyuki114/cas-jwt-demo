# CAS JWT Demo Application

A Spring Boot application that integrates with your existing **CAS (Central Authentication Service)** server to handle **JWE (JSON Web Encryption)** tokens with **A256CBC-HS512** algorithm.

## 🎯 What This Application Does

- **Public Home Page**: Accessible without login at `http://localhost:9446/`
- **CAS Login Integration**: Redirects to your CAS server for authentication
- **JWT Token Processing**: Receives and decrypts JWE tokens from CAS
- **Dashboard**: Shows decrypted JWT payload and user information
- **CAS Logout**: Handles proper logout flow with your CAS server

## 📋 Prerequisites

- **Java 21** or higher
- **Maven 3.6+** 
- **Your CAS Server** running and accessible
- **CAS Admin Access** to register this application as a service

## 🚀 Quick Start Guide

### Step 1: Configure Your CAS Server

You need to register this application in your CAS server. Add this service configuration to your CAS server:

**File**: Save as `demo-service-1001.json` in your CAS services directory (usually `/etc/cas/services/`)

```json
{
  "@class": "org.apereo.cas.services.CasRegisteredService",
  "serviceId": "^http://localhost:9446/.*",
  "name": "demo",
  "id": 1001,
  "description": "CAS Client 1 Demo",
  "evaluationOrder": 1,
  "logoutType": "BACK_CHANNEL",
  "logoutUrl": "http://localhost:9446/logout",
  "attributeReleasePolicy" : {
    "@class" : "org.apereo.cas.services.ReturnLinkedAttributeReleasePolicy",
    "allowedAttributes" : {
      "@class" : "java.util.TreeMap",
      "groupMembership" : ["java.util.ArrayList", ["groupMembership"]]
    }
  },
  "properties" : {
    "@class" : "java.util.HashMap",
    "jwtAsServiceTicket" : {
      "@class" : "org.apereo.cas.services.DefaultRegisteredServiceProperty",
      "values" : [ "java.util.HashSet", [ "true" ] ]
    },
    "jwtAsServiceTicketEncryptionEnabled" : {
      "@class" : "org.apereo.cas.services.DefaultRegisteredServiceProperty",
      "values" : [ "java.util.HashSet", [ "true" ] ]
    },
    "jwtAsServiceTicketSigningEnabled" : {
      "@class" : "org.apereo.cas.services.DefaultRegisteredServiceProperty",
      "values" : [ "java.util.HashSet", [ "true" ] ]
    },
    "jwtAsServiceTicketEncryptionAlg" : {
      "@class" : "org.apereo.cas.services.DefaultRegisteredServiceProperty",
      "values" : [ "java.util.HashSet", [ "A256CBC-HS512" ] ]
    },
    "jwtAsServiceTicketSigningKey" : {
      "@class" : "org.apereo.cas.services.DefaultRegisteredServiceProperty",
      "values" : [ "java.util.HashSet", [ "WAayJAF/KaUxlVC5hwTsXzZiixYCQNnFItaXQlyEdgsydDZaqWPjY+nTZcVEJmJvrHhYlb7syMLRwHMTbd/5XQ==" ] ]
    },
    "jwtAsServiceTicketEncryptionKey" : {
      "@class" : "org.apereo.cas.services.DefaultRegisteredServiceProperty",
      "values" : [ "java.util.HashSet", [ "qt5FuyB+sTHKyQjmTT2MH4FKBxgMvh2/KcE1ja2HgxSbUCF/3nhBMuif6GHNpPJYVCMv1S7MppCm4U7fTxlhGA==" ] ]
    },
    "jwtAsServiceTicketCipherStrategyType" : {
      "@class" : "org.apereo.cas.services.DefaultRegisteredServiceProperty",
      "values" : [ "java.util.HashSet", [ "ENCRYPT_AND_SIGN" ] ]
    }
  }
}

```

**After adding the service file:**
1. Restart your CAS server OR
2. Reload services (if hot-reload is enabled)

### Step 2: Get Your JWT Keys from CAS

You need two keys from your CAS server configuration:

1. **JWT Signing Key** - Used to verify JWT signatures
2. **JWT Encryption Key** - Used to decrypt JWE tokens


### Step 3: Configure This Application

Edit `src/main/resources/application.properties`:

```properties
spring.application.name=cas-jwt-demo
server.port=9446

# CAS Server Configuration - UPDATE THESE URLs
cas.server.url=your-cas-server
cas.service.url=http%3A%2F%2Flocalhost%3A9446%2Flogin%2Fcas
cas.login.url=${cas.server.url}/login?service=${cas.service.url}

# JWT Keys - GET THESE FROM YOUR CAS SERVER
jwt.signing.key=PUT_YOUR_CAS_SIGNING_KEY_HERE
jwt.encryption.key=PUT_YOUR_CAS_ENCRYPTION_KEY_HERE
```

### Step 4: Run the Application

```bash
# Build the project
./mvnw clean compile

# Start the application
./mvnw spring-boot:run
```

### Step 5: Test the Integration

1. **Open your browser**: http://localhost:9446/
2. **Click "Login with CAS"**: Should redirect to your CAS server
3. **Login with your CAS credentials**
4. **View the dashboard**: Should show decrypted JWT token information


## 🚪 How Login Works

### Login Flow
1. User visits http://localhost:9446/
2. Clicks "Login with CAS" → Redirects to your CAS server
3. User authenticates with CAS credentials
4. CAS redirects back with encrypted JWT token
5. Application decrypts token and shows dashboard

