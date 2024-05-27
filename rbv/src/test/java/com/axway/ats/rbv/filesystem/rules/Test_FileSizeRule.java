/*
 * Copyright 2017 Axway Software
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.axway.ats.rbv.filesystem.rules;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.*;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.axway.ats.action.objects.FilePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.rbv.BaseTest;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.filesystem.FileSystemMetaData;
import com.axway.ats.rbv.model.MetaDataIncorrectException;
import com.axway.ats.rbv.model.RbvException;

@RunWith( PowerMockRunner.class)
@PrepareForTest( { FileSizeRule.class })
public class Test_FileSizeRule extends BaseTest {

    public static String             testPath = "/tmp/test/someTestFile.txt";

    public static FileSystemMetaData meta;
    public static FilePackage        pack;

    @Before
    public void setUpTest_FileSizeRule() throws PackageException, RbvException {

        meta = createMock(FileSystemMetaData.class);
        pack = createMock(FilePackage.class);
    }

    @Test
    public void isMatchConstructWithSizePositive() throws Exception {

        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(100L, "isMatchConstructWithSizePositive", true);
        MetaData metaData = new FileSystemMetaData(pack);
        assertTrue(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructWithSizeNegative() throws Exception {

        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(11100L, "isMatchConstructWithSizeNegative", true);
        MetaData metaData = new FileSystemMetaData(pack);
        assertFalse(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructWithSizeExpectFalsePositive() throws Exception {

        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(11100L, "isMatchConstructWithSizeExpectFalsePositive", false);
        MetaData metaData = new FileSystemMetaData(pack);
        assertTrue(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructWithSizeExpectFalseNegative() throws Exception {

        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(100L, "isMatchConstructWithSizeExpectFalseNegative", false);
        MetaData metaData = new FileSystemMetaData(pack);
        assertFalse(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructWithSizeAtLeastPositive() throws Exception {

        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(99L, "isMatchConstructWithSizeAtLeastPositive", 1, false);
        MetaData metaData = new FileSystemMetaData(pack);
        assertTrue(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructWithSizeAtLeastEqualPositive() throws Exception {

        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(100L, "isMatchConstructWithSizeAtLeastEqualPositive", 1, false);
        MetaData metaData = new FileSystemMetaData(pack);
        assertTrue(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructWithSizeAtLeastNegative() throws Exception {

        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(101L, "isMatchConstructWithSizeAtLeastNegative", 1, false);
        MetaData metaData = new FileSystemMetaData(pack);
        assertFalse(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructWithSizeBelowPositive() throws Exception {

        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(99L, "isMatchConstructWithSizeBelowPositive", -1, true);
        MetaData metaData = new FileSystemMetaData(pack);
        assertTrue(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructWithSizeBelowEqualNegative() throws Exception {

        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(100L, "isMatchConstructWithSizeBelowEqualNegative", -1, true);
        MetaData metaData = new FileSystemMetaData(pack);
        assertFalse(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructWithSizeBelowNegative() throws Exception {

        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(101L, "isMatchConstructWithSizeBelowNegative", -1, true);
        MetaData metaData = new FileSystemMetaData(pack);
        assertFalse(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructWithSizeAbovePositive() throws Exception {

        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(101L, "isMatchConstructWithSizeAbovePositive", 1, true);
        MetaData metaData = new FileSystemMetaData(pack);
        assertTrue(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructWithSizeAboveEqualNegative() throws Exception {

        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(100L, "isMatchConstructWithSizeAboveEqualNegative", 1, true);
        MetaData metaData = new FileSystemMetaData(pack);
        assertFalse(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructWithSizeAboveNegative() throws Exception {

        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(99L, "isMatchConstructWithSizeAboveNegative", 1, true);
        MetaData metaData = new FileSystemMetaData(pack);
        assertFalse(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructWithFilePositive() throws Exception {

        expectNew(FilePackage.class, null, null, testPath).andReturn(pack);
        expect(pack.getSize()).andReturn(100L).times(2);

        replayAll();

        FileSizeRule rule = new FileSizeRule(null, testPath, "isMatchConstructWithFilePositive", true);
        MetaData metaData = new FileSystemMetaData(pack);
        assertTrue(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructWithFileNegative() throws Exception {

        expectNew(FilePackage.class, null, null, testPath).andReturn(pack);
        expect(pack.getSize()).andReturn(11100L);
        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(null, testPath, "isMatchConstructWithFileNegative", true);
        MetaData metaData = new FileSystemMetaData(pack);
        assertFalse(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructWithFileExpectFalsePositive() throws Exception {

        expectNew(FilePackage.class, null, null, testPath).andReturn(pack);
        expect(pack.getSize()).andReturn(11100L);
        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(null,
                                             testPath,
                                             "isMatchConstructWithFileExpectFalsePositive",
                                             false);
        MetaData metaData = new FileSystemMetaData(pack);
        assertTrue(rule.isMatch(metaData));

        verifyAll();
    }

    @Test
    public void isMatchConstructWithFileExpectFalseNegative() throws Exception {

        expectNew(FilePackage.class, null, null, testPath).andReturn(pack);
        expect(pack.getSize()).andReturn(100L).times(2);

        replayAll();

        FileSizeRule rule = new FileSizeRule(null,
                                             testPath,
                                             "isMatchConstructWithFileExpectFalseNegative",
                                             false);
        MetaData metaData = new FileSystemMetaData(pack);
        assertFalse(rule.isMatch(metaData));

        verifyAll();
    }

    @Test( expected = MetaDataIncorrectException.class)
    public void isMatchNullMetaDataContent() throws Exception {

        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(100L, "isMatchNullMetaDataContent", true);
        MetaData metaData = new FileSystemMetaData(null);
        assertFalse(rule.isMatch(metaData));

        verifyAll();
    }

    @Test( expected = MetaDataIncorrectException.class)
    public void isMatchInvalidMetaDataContent() throws Exception {

        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(100L, "isMatchInvalidMetaDataContent", true);
        MetaData metaData = new MetaData();
        assertFalse(rule.isMatch(metaData));

        verifyAll();
    }

    @Test( expected = RbvException.class)
    public void isMatchNullMetaData() throws Exception {

        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(100L, "isMatchNullMetaData", true);
        assertFalse(rule.isMatch(null));

        verifyAll();
    }

    @Test
    public void constructWithMachine() throws Exception {

        expectNew(FilePackage.class, null, null, testPath).andReturn(pack);
        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(null, testPath, "constructWithMachine", true);
        assertNotNull(rule);

        verifyAll();
    }

    @Test
    public void constructWithNullMachine() throws Exception {

        expectNew(FilePackage.class, null, null, testPath).andReturn(pack);
        expect(pack.getSize()).andReturn(100L);

        replayAll();

        FileSizeRule rule = new FileSizeRule(null, testPath, "constructWithNullMachine", true);
        assertNotNull(rule);

        verifyAll();
    }

}
