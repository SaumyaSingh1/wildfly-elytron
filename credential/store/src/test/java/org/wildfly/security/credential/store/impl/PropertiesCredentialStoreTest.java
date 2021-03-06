/*
 * Copyright 2021 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.security.credential.store.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.wildfly.security.encryption.SecretKeyUtil.generateSecretKey;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.crypto.SecretKey;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.wildfly.security.credential.SecretKeyCredential;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.security.credential.store.CredentialStoreException;
import org.wildfly.security.credential.store.WildFlyElytronCredentialStoreProvider;

/**
 * Test case to test the {@code PropertiesCredentialStore} implementation.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PropertiesCredentialStoreTest {

    private static final String CREATE = "create";
    private static final String LOCATION = "location";
    private static final String STORE_TYPE = "PropertiesCredentialStore";

    private static final Provider PROVIDER = new WildFlyElytronCredentialStoreProvider();
    private static final Random RANDOM = new Random();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testEmptyStore() throws Exception {
        File storeFile = new File(getStoragePathForNewFile());
        assertFalse(storeFile.exists());
        try {
            CredentialStore credentialStore = CredentialStore.getInstance(STORE_TYPE, () -> new Provider[] { PROVIDER } );
            credentialStore.initialize(toConfigurationMap(storeFile.getAbsolutePath(), true));
            assertTrue(storeFile.exists());

            assertEquals("Alias Count", 0, credentialStore.getAliases().size());

            credentialStore = CredentialStore.getInstance(STORE_TYPE, () -> new Provider[] { PROVIDER } );
            credentialStore.initialize(toConfigurationMap(storeFile.getAbsolutePath(), false));
            assertEquals("Alias Count", 0, credentialStore.getAliases().size());
        } finally {
            if (storeFile.exists()) {
                storeFile.delete();
            }
        }
    }

    @Test
    public void testFileDoesNotExist() throws NoSuchAlgorithmException {
        File storeFile = new File(getStoragePathForNewFile());
        assertFalse(storeFile.exists());
        try {
            CredentialStore credentialStore = CredentialStore.getInstance(STORE_TYPE, () -> new Provider[] { PROVIDER } );
            credentialStore.initialize(toConfigurationMap(storeFile.getAbsolutePath(), false));
            fail("Expected Exception Not Thrown");
        } catch (CredentialStoreException e) {
            assertFalse(storeFile.exists());
            assertTrue("Expected Error Code", e.getMessage().contains("ELY09518"));
        }
    }

    @Test
    public void testSingleSecretKeyEntry() throws Exception {
        final SecretKey secretKey128 = generateSecretKey(128);

        File storeFile = new File(getStoragePathForNewFile());
        assertFalse(storeFile.exists());
        try {
            CredentialStore credentialStore = CredentialStore.getInstance(STORE_TYPE, () -> new Provider[] { PROVIDER });
            credentialStore.initialize(toConfigurationMap(storeFile.getAbsolutePath(), true));
            assertTrue(storeFile.exists());

            assertEquals("Alias Count", 0, credentialStore.getAliases().size());

            credentialStore.store("mySecretKey", new SecretKeyCredential(secretKey128));

            assertEquals("Alias Count", 1, credentialStore.getAliases().size());
            assertTrue("Correct Alias", credentialStore.getAliases().contains("mySecretKey"));
            assertEquals("Returned key", secretKey128, credentialStore.retrieve("mySecretKey", SecretKeyCredential.class).getSecretKey());

            credentialStore.flush();
            credentialStore = CredentialStore.getInstance(STORE_TYPE, () -> new Provider[] { PROVIDER });
            credentialStore.initialize(toConfigurationMap(storeFile.getAbsolutePath(), false));

            assertEquals("Alias Count", 1, credentialStore.getAliases().size());
            assertTrue("Correct Alias", credentialStore.getAliases().contains("mySecretKey"));
            assertEquals("Returned key", secretKey128, credentialStore.retrieve("mySecretKey", SecretKeyCredential.class).getSecretKey());

            credentialStore.remove("mySecretKey", SecretKeyCredential.class);

            assertEquals("Alias Count", 0, credentialStore.getAliases().size());

            credentialStore.flush();
            credentialStore = CredentialStore.getInstance(STORE_TYPE, () -> new Provider[] { PROVIDER });
            credentialStore.initialize(toConfigurationMap(storeFile.getAbsolutePath(), false));

            assertEquals("Alias Count", 0, credentialStore.getAliases().size());
        } finally {
            if (storeFile.exists()) {
                storeFile.delete();
            }
        }
    }


    @Test
    public void testThreeSecretKeyEntries() throws Exception {
        final SecretKey secretKey128 = generateSecretKey(128);
        final SecretKey secretKey192 = generateSecretKey(192);
        final SecretKey secretKey256 = generateSecretKey(256);

        File storeFile = new File(getStoragePathForNewFile());
        assertFalse(storeFile.exists());
        try {
            CredentialStore credentialStore = CredentialStore.getInstance(STORE_TYPE, () -> new Provider[] { PROVIDER });
            credentialStore.initialize(toConfigurationMap(storeFile.getAbsolutePath(), true));
            assertTrue(storeFile.exists());

            assertEquals("Alias Count", 0, credentialStore.getAliases().size());

            credentialStore.store("OneTwoEight", new SecretKeyCredential(secretKey128));
            credentialStore.store("OneNineTwo", new SecretKeyCredential(secretKey192));
            credentialStore.store("TwoFiveSix", new SecretKeyCredential(secretKey256));

            assertEquals("Alias Count", 3, credentialStore.getAliases().size());
            assertTrue("Correct Alias", credentialStore.getAliases().contains("OneTwoEight"));
            assertTrue("Correct Alias", credentialStore.getAliases().contains("OneNineTwo"));
            assertTrue("Correct Alias", credentialStore.getAliases().contains("TwoFiveSix"));
            assertEquals("Returned key", secretKey128, credentialStore.retrieve("OneTwoEight", SecretKeyCredential.class).getSecretKey());
            assertEquals("Returned key", secretKey192, credentialStore.retrieve("OneNineTwo", SecretKeyCredential.class).getSecretKey());
            assertEquals("Returned key", secretKey256, credentialStore.retrieve("TwoFiveSix", SecretKeyCredential.class).getSecretKey());

            credentialStore.flush();
            credentialStore = CredentialStore.getInstance(STORE_TYPE, () -> new Provider[] { PROVIDER });
            credentialStore.initialize(toConfigurationMap(storeFile.getAbsolutePath(), false));

            assertEquals("Alias Count", 3, credentialStore.getAliases().size());
            assertTrue("Correct Alias", credentialStore.getAliases().contains("OneTwoEight"));
            assertTrue("Correct Alias", credentialStore.getAliases().contains("OneNineTwo"));
            assertTrue("Correct Alias", credentialStore.getAliases().contains("TwoFiveSix"));
            assertEquals("Returned key", secretKey128, credentialStore.retrieve("OneTwoEight", SecretKeyCredential.class).getSecretKey());
            assertEquals("Returned key", secretKey192, credentialStore.retrieve("OneNineTwo", SecretKeyCredential.class).getSecretKey());
            assertEquals("Returned key", secretKey256, credentialStore.retrieve("TwoFiveSix", SecretKeyCredential.class).getSecretKey());

            credentialStore.remove("OneNineTwo", SecretKeyCredential.class);

            assertEquals("Alias Count", 2, credentialStore.getAliases().size());
            assertTrue("Correct Alias", credentialStore.getAliases().contains("OneTwoEight"));
            assertTrue("Correct Alias", credentialStore.getAliases().contains("TwoFiveSix"));
            assertEquals("Returned key", secretKey128, credentialStore.retrieve("OneTwoEight", SecretKeyCredential.class).getSecretKey());
            assertEquals("Returned key", secretKey256, credentialStore.retrieve("TwoFiveSix", SecretKeyCredential.class).getSecretKey());

            credentialStore.flush();
            credentialStore = CredentialStore.getInstance(STORE_TYPE, () -> new Provider[] { PROVIDER });
            credentialStore.initialize(toConfigurationMap(storeFile.getAbsolutePath(), false));

            assertEquals("Alias Count", 2, credentialStore.getAliases().size());
            assertTrue("Correct Alias", credentialStore.getAliases().contains("OneTwoEight"));
            assertTrue("Correct Alias", credentialStore.getAliases().contains("TwoFiveSix"));
            assertEquals("Returned key", secretKey128, credentialStore.retrieve("OneTwoEight", SecretKeyCredential.class).getSecretKey());
            assertEquals("Returned key", secretKey256, credentialStore.retrieve("TwoFiveSix", SecretKeyCredential.class).getSecretKey());
        } finally {
            if (storeFile.exists()) {
                storeFile.delete();
            }
        }
    }

    private static Map<String, String> toConfigurationMap(String location, boolean create) {
        Map<String, String> configurationMap = new HashMap<>();
        if (location != null) {
            configurationMap.put(LOCATION, location);
        }

        if (create) {
            configurationMap.put(CREATE, Boolean.TRUE.toString());
        }

        return Collections.unmodifiableMap(configurationMap);
    }

    private String getStoragePathForNewFile() {
        Path path;
        do {
            path = Paths.get(tmp.getRoot().getAbsolutePath(), "/test_" + RANDOM.nextInt() + ".store");
        } while (Files.exists(path));

        return path.toAbsolutePath().toString();
    }

}
