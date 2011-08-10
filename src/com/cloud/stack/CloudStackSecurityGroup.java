/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.stack;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CloudStackSecurityGroup {
    @SerializedName("id")
    private Long id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("account")
    private String accountName;

    @SerializedName("domainid")
    private Long domainId;

    @SerializedName("domain")
    private String domainName;
    
    @SerializedName(ApiConstants.JOB_ID)
    private Long jobId;

    @SerializedName("jobstatus")
    private Integer jobStatus;

    @SerializedName("ingressrule")
    private List<CloudStackIngressRule> ingressRules;

    public CloudStackSecurityGroup() {
    }
    
	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getAccountName() {
		return accountName;
	}

	public Long getDomainId() {
		return domainId;
	}

	public String getDomainName() {
		return domainName;
	}

	public Long getJobId() {
		return jobId;
	}

	public Integer getJobStatus() {
		return jobStatus;
	}

	public List<CloudStackIngressRule> getIngressRules() {
		return ingressRules;
	}
}