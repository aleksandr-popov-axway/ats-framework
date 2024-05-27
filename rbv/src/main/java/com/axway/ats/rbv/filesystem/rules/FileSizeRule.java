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

import java.util.ArrayList;
import java.util.List;

import com.axway.ats.action.objects.FilePackage;
import com.axway.ats.action.objects.model.PackageException;
import com.axway.ats.rbv.MetaData;
import com.axway.ats.rbv.filesystem.FileSystemMetaData;
import com.axway.ats.rbv.model.RbvException;
import com.axway.ats.rbv.model.RbvStorageException;
import com.axway.ats.rbv.rules.AbstractRule;

public class FileSizeRule extends AbstractRule {

    private long size;
    private int cmp = 0;

    /**
     * Match with the specified size
     *
     * @param size The size with which to compare
     * @param ruleName The name of the matcher - used for logging
     * @param cmpResult The result of <code>Long.compare(size, x)</code>
     * @param expectedResult Matcher's expected result
     */
    public FileSizeRule(long size,
                        String ruleName,
                        int cmpResult,
                        boolean expectedResult ) {

        super(ruleName, expectedResult, FileSystemMetaData.class);
        this.size = size;
        this.cmp = cmpResult;
    }

    /**
     * Match with the specified size
     *
     * @param size The size with which to compare
     * @param ruleName The name of the matcher - used for logging
     * @param expectedResult Matcher's expected result
     */
    public FileSizeRule(long size,
                        String ruleName,
                        boolean expectedResult ) {

        this(size, ruleName, 0, expectedResult);
    }

    /**
     * Match with the size of the specified file.
     *
     * @param atsAgent the address of the remote ATS agent
     * @param filePath the path name of the file for comparison
     * @param cmpResult The result of size.compareTo(x)
     * @param ruleName the rule name
     * @param expectedResult the expected result
     * @throws RbvException specified file size is undefined
     */
    public FileSizeRule(String atsAgent,
                        String filePath,
                        String ruleName,
                        int cmpResult,
                        boolean expectedResult ) throws RbvException {

        super(ruleName, expectedResult, FileSystemMetaData.class);

        try {
            //get source file's size
            FilePackage file = new FilePackage(atsAgent, filePath);
            this.size = file.getSize();

        } catch (PackageException pe) {
            throw new RbvStorageException(pe);
        }

        this.cmp = cmpResult;
    }

    /**
     * Match with the size of the specified file.
     *
     * @param atsAgent the address of the remote ATS agent
     * @param filePath the path name of the file for comparison
     * @param ruleName the rule name
     * @param expectedResult the expected result
     * @throws RbvException specified file size is undefined
     */
    public FileSizeRule(String atsAgent,
                        String filePath,
                        String ruleName,
                        boolean expectedResult ) throws RbvException {

        this(atsAgent, filePath, ruleName, 0, expectedResult);

    }

    @Override
    public boolean performMatch( MetaData metaData ) throws RbvException {

        boolean actuaResult = false;

        if (metaData instanceof FileSystemMetaData) {
            try {
                //get the file from the metadata
                FilePackage file = ((FileSystemMetaData) metaData).getFilePackage();

                //get destination file's size
                long fileSize = file.getSize();

                actuaResult = Long.compare(this.size, fileSize) == this.cmp;
            } catch (PackageException pe) {
                throw new RbvStorageException(pe);
            }
        }
        return actuaResult;
    }

    @Override
    protected String getRuleDescription() {

        String relativeDesc = "";

        if (this.cmp != 0) {
            if (!getExpectedResult())
                relativeDesc += "not ";
            relativeDesc += (this.cmp < 0) ? "below" : "above";
        } else if (!getExpectedResult()) {
            relativeDesc = "different than";
        }

        return String.format("which expects file with size %s '%d'", relativeDesc, this.size);
    }

    public List<String> getMetaDataKeys() {

        List<String> metaKeys = new ArrayList<String>();
        metaKeys.add(FileSystemMetaData.FILE_PACKAGE);
        return metaKeys;
    }
}
