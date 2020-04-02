package io.axoniq.axonserver.enterprise.storage.file;

import io.axoniq.axonserver.enterprise.config.AxonServerEnterpriseProperties;
import io.axoniq.axonserver.exception.ErrorCode;
import io.axoniq.axonserver.exception.MessagingPlatformException;
import io.axoniq.axonserver.licensing.LicenseException;
import io.axoniq.axonserver.licensing.LicensePropertyReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

@Component
public class LicenseManager {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AxonServerEnterpriseProperties axonServerEnterpriseProperties;

    private final String LICENSE_FILENAME = "axoniq.license";

    public LicenseManager(AxonServerEnterpriseProperties axonServerEnterpriseProperties) {
        this.axonServerEnterpriseProperties = axonServerEnterpriseProperties;
    }

    public void createOrUpdate(byte[] license) {

        logger.info("Validating new license...");

        Properties licenseProperties = load(license);
        validate(licenseProperties);

        File path = new File(axonServerEnterpriseProperties.getLicenseDirectory());
        if (!path.exists() && !path.mkdirs()) {
            throw new MessagingPlatformException(ErrorCode.OTHER,
                    "Failed to create directory: " + path.getAbsolutePath());
        }

        try (FileOutputStream out = new FileOutputStream(path.getAbsolutePath() + "/" + LICENSE_FILENAME)) {
            out.write(license);
        } catch (IOException e) {
            throw LicenseException.unableToWrite(path,e);
        }

        logger.info("New license saved!");
    }

    public Properties readLicenseProperties() {
        String licenseFile = System.getProperty("license", System.getenv("AXONIQ_LICENSE"));
        if (licenseFile == null) {
            licenseFile = axonServerEnterpriseProperties.getLicenseDirectory() + "/" + LICENSE_FILENAME;
            File file = new File(licenseFile);
            if (!file.exists()) return null;
        }
        Properties licenseProperties = load(licenseFile);
        validate(licenseProperties);

        return licenseProperties;
    }

    private Properties load(String licenseFile) {
        logger.info("Loading license...");

        File file = new File(licenseFile);
        Properties licenseProperties = new Properties();
        try {
            licenseProperties.load(new FileInputStream(file));
        } catch (IOException ex) {
            throw LicenseException.unableToRead(file);
        }
        return licenseProperties;
    }

    private Properties load(byte[] licenseFile) {
        logger.info("Loading license...");

        Properties licenseProperties = new Properties();
        try {
            licenseProperties.load( new ByteArrayInputStream(licenseFile));
        } catch (IOException ex) {
            throw LicenseException.noLicenseFile();
        }
        return licenseProperties;
    }

    private void validate(Properties licenseProperties) {
        try {
            KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
            byte[] pubKeyBytes = Base64.getMimeDecoder().decode(PUBKEY);
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pubKeyBytes);
            PublicKey pubKey = rsaKeyFactory.generatePublic(pubKeySpec);
            Signature rsaVerify = Signature.getInstance("SHA256withRSA");
            rsaVerify.initVerify(pubKey);
            List<String> propnames = new ArrayList<>(licenseProperties.stringPropertyNames());
            Collections.sort(propnames);
            if (!propnames.remove("signature")) throw LicenseException.wrongSignature("signature field missing");
            for (String propname : propnames) {
                rsaVerify.update(propname.getBytes(StandardCharsets.UTF_8));
                rsaVerify.update(licenseProperties.getProperty(propname, "").getBytes(StandardCharsets.UTF_8));
            }
            boolean verifies = rsaVerify.verify(Base64.getDecoder().decode(licenseProperties.getProperty("signature")));
            if (!verifies) {
                throw LicenseException.wrongSignature("signature invalid");
            }
        } catch (SignatureException ex) {
            throw LicenseException.wrongSignature("SignatureException: " + ex.getMessage());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException ex) {
            throw new Error("This should never happen", ex);
        }
    }

    private static final String PUBKEY =
            "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA0xyNKA5aL42X7eXy1zwe\n" +
                    "9V6do3TYhrH7smWa6RBtCkhQ2holCEalrdiEX3LQoyhPmvV8lqWrDc9JHuYheWQL\n" +
                    "pQXKB84sb9DCCWZWTPV0OZpe8nyotgmwBYohvEzTGLLRrAp+pM/J+/IVSiMyiP5E\n" +
                    "Kf6ODcRWQH/us+4x4IsjTZC+o0HsYjSXG62Bo7pXFXLKjUqA3rpTyT1v3Yafgp4C\n" +
                    "78wHa/fqKCE562B2IEEhxWdsJl//wOsk/I8bYH+YoZtceGpRJlkMjK3t/KOExU61\n" +
                    "ae5NJruyXbqRBWOtrcBb37b2cgykqaZlCwQczsZwl8Pglm1Yl0t8lTdTM+wxLErI\n" +
                    "AbxYE50UtvMLCaIG8lqT9W28UQgOr+RPdkEwUWYNeWWH2R0Kukva9loB+LBDe/Ce\n" +
                    "YhRvh41KpekJhU0NYjymCizNFohQ0rUDtt8p+i/IpIxfWBtgJODOrP2tbr8necX8\n" +
                    "X5oMyN4H/ar6favdWCHXi9FtTrHv1lchisXn3R9/obJptkxyZc8yvWuEBhXBFJ6H\n" +
                    "ydOPNdbiWIH9TptZ2vaQrSFyaPR5yCoG/kyZ6o7TQE8lK6MrULiJNB/6ZKujri5x\n" +
                    "LovNJrtY/w69qVkC/8lIJhwJMSJKySeUYBhOjVN4f7vVEVYncYx8HJU2utQ1j6+e\n" +
                    "9T0pQ8CjhkOpmcTcaaMmU0UCAwEAAQ==";


}
