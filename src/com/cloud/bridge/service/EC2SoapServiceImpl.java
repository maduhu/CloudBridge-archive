/*
 * Copyright 2010 Cloud.com, Inc.
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
package com.cloud.bridge.service;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.amazon.ec2.*;
import com.cloud.bridge.service.core.ec2.EC2AuthorizeRevokeSecurityGroup;
import com.cloud.bridge.service.core.ec2.EC2CreateImage;
import com.cloud.bridge.service.core.ec2.EC2CreateImageResponse;
import com.cloud.bridge.service.core.ec2.EC2CreateVolume;
import com.cloud.bridge.service.core.ec2.EC2DescribeAvailabilityZones;
import com.cloud.bridge.service.core.ec2.EC2DescribeAvailabilityZonesResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeImages;
import com.cloud.bridge.service.core.ec2.EC2DescribeImagesResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeInstances;
import com.cloud.bridge.service.core.ec2.EC2DescribeInstancesResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeSecurityGroups;
import com.cloud.bridge.service.core.ec2.EC2DescribeSecurityGroupsResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeSnapshots;
import com.cloud.bridge.service.core.ec2.EC2DescribeSnapshotsResponse;
import com.cloud.bridge.service.core.ec2.EC2DescribeVolumes;
import com.cloud.bridge.service.core.ec2.EC2DescribeVolumesResponse;
import com.cloud.bridge.service.core.ec2.EC2Engine;
import com.cloud.bridge.service.core.ec2.EC2Filter;
import com.cloud.bridge.service.core.ec2.EC2GroupFilterSet;
import com.cloud.bridge.service.core.ec2.EC2Image;
import com.cloud.bridge.service.core.ec2.EC2Instance;
import com.cloud.bridge.service.core.ec2.EC2InstanceFilterSet;
import com.cloud.bridge.service.core.ec2.EC2IpPermission;
import com.cloud.bridge.service.core.ec2.EC2PasswordData;
import com.cloud.bridge.service.core.ec2.EC2RebootInstances;
import com.cloud.bridge.service.core.ec2.EC2RegisterImage;
import com.cloud.bridge.service.core.ec2.EC2RunInstances;
import com.cloud.bridge.service.core.ec2.EC2RunInstancesResponse;
import com.cloud.bridge.service.core.ec2.EC2SSHKeyPair;
import com.cloud.bridge.service.core.ec2.EC2SecurityGroup;
import com.cloud.bridge.service.core.ec2.EC2Snapshot;
import com.cloud.bridge.service.core.ec2.EC2SnapshotFilterSet;
import com.cloud.bridge.service.core.ec2.EC2StartInstances;
import com.cloud.bridge.service.core.ec2.EC2StartInstancesResponse;
import com.cloud.bridge.service.core.ec2.EC2StopInstances;
import com.cloud.bridge.service.core.ec2.EC2StopInstancesResponse;
import com.cloud.bridge.service.core.ec2.EC2Volume;
import com.cloud.bridge.service.core.ec2.EC2DescribeAddresses;
import com.cloud.bridge.service.core.ec2.EC2VolumeFilterSet;
import com.cloud.bridge.service.exception.EC2ServiceException;
import com.cloud.bridge.service.exception.EC2ServiceException.ClientError;


public class EC2SoapServiceImpl implements AmazonEC2SkeletonInterface  {
    protected final static Logger logger = Logger.getLogger(EC2SoapServiceImpl.class);

    private static EC2Engine engine;
    
    public EC2SoapServiceImpl(EC2Engine engine) {
    	this.engine = engine;
    }

	public AttachVolumeResponse attachVolume(AttachVolume attachVolume) {
		EC2Volume request = new EC2Volume();
		AttachVolumeType avt = attachVolume.getAttachVolume();
		
		request.setId( avt.getVolumeId());
		request.setInstanceId( avt.getInstanceId());
		request.setDevice( avt.getDevice());
		return toAttachVolumeResponse( engine.attachVolume( request ));
	}
	
	public AuthorizeSecurityGroupIngressResponse authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngress authorizeSecurityGroupIngress) {
        AuthorizeSecurityGroupIngressType sgit = authorizeSecurityGroupIngress.getAuthorizeSecurityGroupIngress();        
        IpPermissionSetType ipPerms = sgit.getIpPermissions();
        
        EC2AuthorizeRevokeSecurityGroup request = toSecurityGroup( sgit.getGroupName(), ipPerms.getItem());
		return toAuthorizeSecurityGroupIngressResponse( engine.securityGroupRequest( request, "authorizeSecurityGroupIngress" ));
	}

	
	public RevokeSecurityGroupIngressResponse revokeSecurityGroupIngress( RevokeSecurityGroupIngress revokeSecurityGroupIngress ) 
	{
        RevokeSecurityGroupIngressType sgit = revokeSecurityGroupIngress.getRevokeSecurityGroupIngress();        
        IpPermissionSetType ipPerms = sgit.getIpPermissions();
        
        EC2AuthorizeRevokeSecurityGroup request = toSecurityGroup( sgit.getGroupName(), ipPerms.getItem());
		return toRevokeSecurityGroupIngressResponse( engine.revokeSecurityGroup( request ));
	}

	
	/**
	 * Authorize and Revoke Security Group Ingress have the same parameters.
	 */
	private EC2AuthorizeRevokeSecurityGroup toSecurityGroup( String groupName, IpPermissionType[] items ) {
        EC2AuthorizeRevokeSecurityGroup request = new  EC2AuthorizeRevokeSecurityGroup();

        request.setName( groupName );
         
        for( int i=0; i < items.length; i++ ) {
    	   EC2IpPermission perm = new EC2IpPermission();       	
    	   perm.setProtocol( items[i].getIpProtocol());
    	   perm.setFromPort( items[i].getFromPort());
    	   perm.setToPort(   items[i].getToPort());
  	
    	   UserIdGroupPairSetType groups = items[i].getGroups();
    	   if (null != groups) {
    		   UserIdGroupPairType[] groupItems = groups.getItem();
    		   for( int j=0; null != groupItems && j < groupItems.length; j++ ) {
    			  EC2SecurityGroup user = new EC2SecurityGroup();
    			  user.setName( groupItems[j].getGroupName());
    			  user.setAccount( groupItems[j].getUserId());
    			  perm.addUser( user );
    		   }    		
    	   }     	
   
    	   IpRangeSetType ranges = items[i].getIpRanges();
    	   if (null != ranges) {
    		   IpRangeItemType[] rangeItems = ranges.getItem();
    		   for( int k=0; null != rangeItems && k < rangeItems.length; k++ ) 
    			  perm.addIpRange( rangeItems[k].getCidrIp());
    	   }  
   
    	   request.addIpPermission( perm );
        }
        return request;
    }

	public CreateImageResponse createImage(CreateImage createImage) {
		EC2CreateImage request = new EC2CreateImage();
		CreateImageType cit = createImage.getCreateImage();
		
		request.setInstanceId( cit.getInstanceId());
		request.setName( cit.getName());
		request.setDescription( cit.getDescription());
		return toCreateImageResponse( engine.handleRequest( request ));
	}

	public CreateSecurityGroupResponse createSecurityGroup(CreateSecurityGroup createSecurityGroup) {
        CreateSecurityGroupType sgt = createSecurityGroup.getCreateSecurityGroup();
        EC2SecurityGroup request = new EC2SecurityGroup();
        	
        request.setName( sgt.getGroupName());
        request.setDescription( sgt.getGroupDescription());
		return toCreateSecurityGroupResponse( engine.createSecurityGroup( request ));
	}

	public CreateSnapshotResponse createSnapshot(CreateSnapshot createSnapshot) {
		CreateSnapshotType cst = createSnapshot.getCreateSnapshot();
		return toCreateSnapshotResponse( engine.createSnapshot( cst.getVolumeId()), engine);
	}

	public CreateVolumeResponse createVolume(CreateVolume createVolume) {
		EC2CreateVolume request = new EC2CreateVolume();
		CreateVolumeType cvt = createVolume.getCreateVolume();
		
		request.setSize( cvt.getSize());
		request.setSnapshotId( cvt.getSnapshotId());
		request.setZoneName( cvt.getAvailabilityZone());
		return toCreateVolumeResponse( engine.handleRequest( request ));
	}

	public DeleteSecurityGroupResponse deleteSecurityGroup(DeleteSecurityGroup deleteSecurityGroup) {
        DeleteSecurityGroupType sgt = deleteSecurityGroup.getDeleteSecurityGroup();
        EC2SecurityGroup request = new EC2SecurityGroup();
        
        request.setName( sgt.getGroupName());
		return toDeleteSecurityGroupResponse( engine.deleteSecurityGroup( request ));
	}

	public DeleteSnapshotResponse deleteSnapshot(DeleteSnapshot deleteSnapshot) {
		DeleteSnapshotType dst = deleteSnapshot.getDeleteSnapshot();		
		return toDeleteSnapshotResponse( engine.deleteSnapshot( dst.getSnapshotId()));
	}

	public DeleteVolumeResponse deleteVolume(DeleteVolume deleteVolume) {
		EC2Volume request = new EC2Volume();
		DeleteVolumeType avt = deleteVolume.getDeleteVolume();
		
		request.setId( avt.getVolumeId());
		return toDeleteVolumeResponse( engine.deleteVolume( request ));
	}

	public DeregisterImageResponse deregisterImage(DeregisterImage deregisterImage) {
		DeregisterImageType dit = deregisterImage.getDeregisterImage();
		EC2Image image = new EC2Image();
		
		image.setId( dit.getImageId());
		return toDeregisterImageResponse( engine.deregisterImage( image ));
	}

	public DescribeAvailabilityZonesResponse describeAvailabilityZones(DescribeAvailabilityZones describeAvailabilityZones) {
		EC2DescribeAvailabilityZones request = new EC2DescribeAvailabilityZones();
		
		DescribeAvailabilityZonesType dazt = describeAvailabilityZones.getDescribeAvailabilityZones();
		DescribeAvailabilityZonesSetType dazs = dazt.getAvailabilityZoneSet();
		DescribeAvailabilityZonesSetItemType[] items = dazs.getItem();
		if (null != items) {  // -> can be empty
			for( int i=0; i < items.length; i++ ) request.addZone( items[i].getZoneName());
		}
		return toDescribeAvailabilityZonesResponse( engine.handleRequest( request ));
	}

	/**
	 * This only supports a query about description.
	 */
	public DescribeImageAttributeResponse describeImageAttribute(DescribeImageAttribute describeImageAttribute) {
		EC2DescribeImages request = new EC2DescribeImages();
		DescribeImageAttributeType diat = describeImageAttribute.getDescribeImageAttribute();
		DescribeImageAttributesGroup diag = diat.getDescribeImageAttributesGroup();
		EmptyElementType description = diag.getDescription();

		if ( null != description ) {
			 request.addImageSet( diat.getImageId());
		     return toDescribeImageAttributeResponse( engine.handleRequest( request ));
		}
		else throw new EC2ServiceException( "Unsupported - only description supported", 501 );
	}

	public DescribeImagesResponse describeImages(DescribeImages describeImages) {
		EC2DescribeImages  request = new EC2DescribeImages();
		DescribeImagesType dit     = describeImages.getDescribeImages();
		
		// -> toEC2DescribeImages
	    DescribeImagesExecutableBySetType param1 = dit.getExecutableBySet();
	    if (null != param1) {
	        DescribeImagesExecutableByType[] items1  = param1.getItem();
	        if (null != items1) { 
		        for( int i=0; i < items1.length; i++ ) request.addExecutableBySet( items1[i].getUser());
	        }
	    }
		DescribeImagesInfoType param2 = dit.getImagesSet();
		if (null != param2) {
		    DescribeImagesItemType[] items2 = param2.getItem();
		    if (null != items2) {  
		        for( int i=0; i < items2.length; i++ ) request.addImageSet( items2[i].getImageId());
		    }
		}
		DescribeImagesOwnersType param3 = dit.getOwnersSet();
		if (null != param3) {
		    DescribeImagesOwnerType[] items3 = param3.getItem();
		    if (null != items3) {  
			    for( int i=0; i < items3.length; i++ ) request.addOwnersSet( items3[i].getOwner());
		    }
		}    

		return toDescribeImagesResponse( engine.handleRequest( request ));
	}

	public DescribeInstanceAttributeResponse describeInstanceAttribute(DescribeInstanceAttribute describeInstanceAttribute) {
	    EC2DescribeInstances  request = new EC2DescribeInstances();
	    DescribeInstanceAttributeType diat = describeInstanceAttribute.getDescribeInstanceAttribute();
	    DescribeInstanceAttributesGroup diag = diat.getDescribeInstanceAttributesGroup();
	    EmptyElementType instanceType = diag.getInstanceType();
		
	    // -> toEC2DescribeInstances
	    if (null != instanceType) {
		    request.addInstanceId( diat.getInstanceId());
		    return toDescribeInstanceAttributeResponse( engine.handleRequest( request ));
	    }
	    throw new EC2ServiceException( "Unsupported - only instanceType supported", 501 );
	}

	
	public DescribeInstancesResponse describeInstances( DescribeInstances describeInstances ) 
	{
		EC2DescribeInstances  request = new EC2DescribeInstances();
		DescribeInstancesType dit     = describeInstances.getDescribeInstances();
		FilterSetType fst = dit.getFilterSet();

		// -> toEC2DescribeInstances
		DescribeInstancesInfoType   diit  = dit.getInstancesSet();
		DescribeInstancesItemType[] items = diit.getItem();
		if (null != items) {  // -> can be empty
			for( int i=0; i < items.length; i++ ) request.addInstanceId( items[i].getInstanceId());
		}
		
		if (null != fst) {
			request.setFilterSet( toInstanceFilterSet( fst ));
		}
		
		return toDescribeInstancesResponse( engine.handleRequest( request ), engine );
	}

	
    @Override
    public DescribeAddressesResponse describeAddresses(DescribeAddresses describeAddresses) {
        EC2DescribeAddresses request = new EC2DescribeAddresses();
        DescribeAddressesType dat = describeAddresses.getDescribeAddresses();

        DescribeAddressesInfoType dait = dat.getPublicIpsSet();
        DescribeAddressesItemType[] items = dait.getItem();
        if (null != items) {  // -> can be empty
            for( int i=0; i < items.length; i++ ) request.addPublicIp( items[i].getPublicIp());
        }
        return toDescribeAddressesResponse( engine.describeAddresses( request.getPublicIpsSet() ));
    }

    @Override
    public AllocateAddressResponse allocateAddress(AllocateAddress allocateAddress) {
        return toAllocateAddressResponse( engine.allocateAddress() );
    }

    @Override
    public ReleaseAddressResponse releaseAddress(ReleaseAddress releaseAddress) {
        String publicIp = releaseAddress.getReleaseAddress().getPublicIp();
        return toReleaseAddressResponse( engine.releaseAddress(publicIp) );
    }

    @Override
    public AssociateAddressResponse associateAddress(AssociateAddress associateAddress) {
        String publicIp   = associateAddress.getAssociateAddress().getPublicIp();
        String instanceId = associateAddress.getAssociateAddress().getInstanceId();
        return toAssociateAddressResponse( engine.associateAddress(publicIp, instanceId) );
    }

    @Override
    public DisassociateAddressResponse disassociateAddress(DisassociateAddress disassociateAddress) {
        String publicIp = disassociateAddress.getDisassociateAddress().getPublicIp();
        return toDisassociateAddressResponse( engine.disassociateAddress(publicIp) );
    }

    
	public DescribeSecurityGroupsResponse describeSecurityGroups(DescribeSecurityGroups describeSecurityGroups) 
	{
	    EC2DescribeSecurityGroups request = new EC2DescribeSecurityGroups();
        DescribeSecurityGroupsType sgt = describeSecurityGroups.getDescribeSecurityGroups();
		FilterSetType fst = sgt.getFilterSet();

		// -> toEC2DescribeSecurityGroups
        DescribeSecurityGroupsSetType sgst = sgt.getSecurityGroupSet();
        DescribeSecurityGroupsSetItemType[] items = sgst.getItem();
		if (null != items) {  // -> can be empty
			for( int i=0; i < items.length; i++ ) request.addGroupName( items[i].getGroupName());
		}
		
		if (null != fst) {
			request.setFilterSet( toGroupFilterSet( fst ));
		}

		return toDescribeSecurityGroupsResponse( engine.handleRequest( request ));
	}

	
	public DescribeSnapshotsResponse describeSnapshots(DescribeSnapshots describeSnapshots) 
	{
		EC2DescribeSnapshots request = new EC2DescribeSnapshots();
		DescribeSnapshotsType dst = describeSnapshots.getDescribeSnapshots();
	
		DescribeSnapshotsSetType dsst = dst.getSnapshotSet();
		FilterSetType fst = dst.getFilterSet();

		if (null != dsst) 
		{
			DescribeSnapshotsSetItemType[] items = dsst.getItem();
            if (null != items) {
			    for( int i=0; i < items.length; i++ ) request.addSnapshotId( items[i].getSnapshotId());
            }
		}
		
		if (null != fst) 
		{
			String[] timeFilters = new String[1];
			timeFilters[0] = new String( "start-time" );
			request.setFilterSet( toSnapshotFilterSet( fst, timeFilters ));
		}

		return toDescribeSnapshotsResponse(engine.handleRequest(request));
	}

	
	public DescribeVolumesResponse describeVolumes(DescribeVolumes describeVolumes) 
	{
		EC2DescribeVolumes request = new EC2DescribeVolumes();
		DescribeVolumesType dvt = describeVolumes.getDescribeVolumes();
		
		DescribeVolumesSetType dvst = dvt.getVolumeSet();
		FilterSetType fst = dvt.getFilterSet();
		
		if (null != dvst) 
		{
		    DescribeVolumesSetItemType[] items = dvst.getItem();
		    if (null != items) {
		    	for( int i=0; i < items.length; i++ ) request.addVolumeId( items[i].getVolumeId());
		    }
		}	
		
		if (null != fst) 
		{
			String[] timeFilters = new String[2];
			timeFilters[0] = new String( "attachment.attach-time" );
			timeFilters[1] = new String( "create-time"            );
			request.setFilterSet( toVolumeFilterSet( fst, timeFilters ));
		}
		
		return toDescribeVolumesResponse( engine.handleRequest( request ));
	}
	
	public DetachVolumeResponse detachVolume(DetachVolume detachVolume) {
		EC2Volume request = new EC2Volume();
		DetachVolumeType avt = detachVolume.getDetachVolume();
		
		request.setId( avt.getVolumeId());
		request.setInstanceId( avt.getInstanceId());
		request.setDevice( avt.getDevice());
		return toDetachVolumeResponse( engine.detachVolume( request ));
	}

	public ModifyImageAttributeResponse modifyImageAttribute(ModifyImageAttribute modifyImageAttribute) {
		EC2Image request = new EC2Image();
		ModifyImageAttributeType miat = modifyImageAttribute.getModifyImageAttribute();
		ModifyImageAttributeTypeChoice_type0 item = miat.getModifyImageAttributeTypeChoice_type0();
		AttributeValueType description = item.getDescription();
		
		if (null != description) {
		    request.setId( miat.getImageId());
		    request.setDescription(description.getValue());
		    return toModifyImageAttributeResponse( engine.modifyImageAttribute( request ));
		}
		throw new EC2ServiceException( "Unsupported - can only modify image description", 501 );
	}	

	/**
	 * Did not find a matching service offering so for now we just return disabled
	 * for each instance request.  We could verify that all of the specified instances
	 * exist to detect an error which would require a listVirtualMachines.
	 */
	public MonitorInstancesResponse monitorInstances(MonitorInstances monitorInstances) {
		MonitorInstancesResponse response = new MonitorInstancesResponse();
		MonitorInstancesResponseType param1 = new MonitorInstancesResponseType();
		MonitorInstancesResponseSetType param2 = new MonitorInstancesResponseSetType();
 		
		MonitorInstancesType mit = monitorInstances.getMonitorInstances();
		MonitorInstancesSetType mist = mit.getInstancesSet();
		MonitorInstancesSetItemType[] misit = mist.getItem();
		
		if (null != misit) {  
			for( int i=0; i < misit.length; i++ ) {
				String instanceId = misit[i].getInstanceId();
				MonitorInstancesResponseSetItemType param3 = new MonitorInstancesResponseSetItemType();	
				param3.setInstanceId( instanceId );
				InstanceMonitoringStateType param4 = new InstanceMonitoringStateType();
				param4.setState( "disabled" );
				param3.setMonitoring( param4 );
				param2.addItem( param3 );
			}
		}

		param1.setRequestId( UUID.randomUUID().toString());
        param1.setInstancesSet( param2 );
		response.setMonitorInstancesResponse( param1 );
		return response;
	}

	public RebootInstancesResponse rebootInstances(RebootInstances rebootInstances) {
		EC2RebootInstances request = new EC2RebootInstances();
		RebootInstancesType rit = rebootInstances.getRebootInstances();
		
		// -> toEC2StartInstances
		RebootInstancesInfoType   rist  = rit.getInstancesSet();
		RebootInstancesItemType[] items = rist.getItem();
		if (null != items) {  // -> should not be empty
			for( int i=0; i < items.length; i++ ) request.addInstanceId( items[i].getInstanceId());
		}
		return toRebootInstancesResponse( engine.handleRequest( request ));
	}

	public RegisterImageResponse registerImage(RegisterImage registerImage) {
		EC2RegisterImage request = new EC2RegisterImage();
		RegisterImageType rit = registerImage.getRegisterImage();
		
		// -> we redefine the architecture field to hold: "format:zonename:osTypeName",
		//    these are the bare minimum that we need to call the cloud registerTemplate call.
		request.setLocation( rit.getImageLocation());   // -> should be a URL for us
		request.setName( rit.getName());
		request.setDescription( rit.getDescription());
		request.setArchitecture( rit.getArchitecture());  
		return toRegisterImageResponse( engine.handleRequest( request ));
	}

	public ResetImageAttributeResponse resetImageAttribute(ResetImageAttribute resetImageAttribute) {
		EC2Image request = new EC2Image();
		ResetImageAttributeType riat = resetImageAttribute.getResetImageAttribute();
		
		request.setId( riat.getImageId());
		request.setDescription( "" );
		return toResetImageAttributeResponse( engine.modifyImageAttribute( request ));
	}

	public RunInstancesResponse runInstances(RunInstances runInstances) {
		EC2RunInstances request = new EC2RunInstances();
		RunInstancesType rit = runInstances.getRunInstances();
		PlacementRequestType prt = rit.getPlacement();
		GroupSetType gst = rit.getGroupSet();
		String type = rit.getInstanceType();	
		UserDataType userData = rit.getUserData();
		String keyName = rit.getKeyName();
		
		request.setTemplateId( rit.getImageId());
		request.setMinCount( rit.getMinCount());
		request.setMaxCount( rit.getMaxCount());
		if (null != type    ) request.setInstanceType( type );
		if (null != prt     ) request.setZoneName( prt.getAvailabilityZone());
		if (null != userData) request.setUserData( userData.getData());
		if (null != keyName ) request.setKeyName( keyName );
		
		// -> we can only support one group per instance
		if (null != gst) {
			GroupItemType[] items = gst.getItem();
			if (null != items && 0 < items.length) request.setGroupId( items[0].getGroupId());
		}
		return toRunInstancesResponse( engine.handleRequest( request ), engine);
	}
	
	public StartInstancesResponse startInstances(StartInstances startInstances) {
		EC2StartInstances request = new EC2StartInstances();
		StartInstancesType sit = startInstances.getStartInstances();
		
		// -> toEC2StartInstances
		InstanceIdSetType iist  = sit.getInstancesSet();
		InstanceIdType[]  items = iist.getItem();
		if (null != items) {  // -> should not be empty
			for( int i=0; i < items.length; i++ ) request.addInstanceId( items[i].getInstanceId());
		}
		return toStartInstancesResponse( engine.handleRequest( request ));
	}
	
	public StopInstancesResponse stopInstances(StopInstances stopInstances) {
		EC2StopInstances request = new EC2StopInstances();
		StopInstancesType sit = stopInstances.getStopInstances();
		
		// -> toEC2StopInstances
		InstanceIdSetType iist  = sit.getInstancesSet();
		InstanceIdType[]  items = iist.getItem();
		if (null != items) {  // -> should not be empty
			for( int i=0; i < items.length; i++ ) request.addInstanceId( items[i].getInstanceId());
		}
		return toStopInstancesResponse( engine.handleRequest( request ));
	}
	
	/**
	 * Mapping this to the destroyVirtualMachine cloud API concept.
	 * This makes sense since when considering the rebootInstances function.   In reboot
	 * any terminated instances are left alone.   We will do the same with destroyed instances.
	 */
	public TerminateInstancesResponse terminateInstances(TerminateInstances terminateInstances) {
		EC2StopInstances request = new EC2StopInstances();
		TerminateInstancesType sit = terminateInstances.getTerminateInstances();
		
		// -> toEC2StopInstances
		InstanceIdSetType iist  = sit.getInstancesSet();
		InstanceIdType[]  items = iist.getItem();
		if (null != items) {  // -> should not be empty
			for( int i=0; i < items.length; i++ ) request.addInstanceId( items[i].getInstanceId());
		}

		request.setDestroyInstances( true );
		return toTermInstancesResponse( engine.handleRequest( request ));
	}
	
	/**
	 * See comment for monitorInstances.
	 */
	public UnmonitorInstancesResponse unmonitorInstances(UnmonitorInstances unmonitorInstances) {
		UnmonitorInstancesResponse response = new UnmonitorInstancesResponse();
		MonitorInstancesResponseType param1 = new MonitorInstancesResponseType();
		MonitorInstancesResponseSetType param2 = new MonitorInstancesResponseSetType();
 		
		MonitorInstancesType mit = unmonitorInstances.getUnmonitorInstances();
		MonitorInstancesSetType mist = mit.getInstancesSet();
		MonitorInstancesSetItemType[] items = mist.getItem();
		
		if (null != items) {  
			for( int i=0; i < items.length; i++ ) {
				String instanceId = items[i].getInstanceId();
				MonitorInstancesResponseSetItemType param3 = new MonitorInstancesResponseSetItemType();	
				param3.setInstanceId( instanceId );
				InstanceMonitoringStateType param4 = new InstanceMonitoringStateType();
				param4.setState( "disabled" );
				param3.setMonitoring( param4 );
				param2.addItem( param3 );
			}
		}

        param1.setInstancesSet( param2 );
		param1.setRequestId( UUID.randomUUID().toString());
		response.setUnmonitorInstancesResponse( param1 );
		return response;
	}
	
	
	public static DescribeImageAttributeResponse toDescribeImageAttributeResponse(EC2DescribeImagesResponse engineResponse) {
		DescribeImageAttributeResponse response = new DescribeImageAttributeResponse();
		DescribeImageAttributeResponseType param1 = new DescribeImageAttributeResponseType();
		
		EC2Image[] imageSet = engineResponse.getImageSet();
		if ( 0 < imageSet.length ) {
		     DescribeImageAttributeResponseTypeChoice_type0 param2 = new DescribeImageAttributeResponseTypeChoice_type0();
		     NullableAttributeValueType param3 = new NullableAttributeValueType();
		     param3.setValue( imageSet[0].getDescription());
		     param2.setDescription( param3 );
		     param1.setDescribeImageAttributeResponseTypeChoice_type0( param2 );
		     param1.setImageId( imageSet[0].getId());	
		}
		
		param1.setRequestId( UUID.randomUUID().toString());
        response.setDescribeImageAttributeResponse( param1 );
		return response;
	}
	
	public static ModifyImageAttributeResponse toModifyImageAttributeResponse( boolean engineResponse ) {
		ModifyImageAttributeResponse response = new ModifyImageAttributeResponse();
		ModifyImageAttributeResponseType param1 = new ModifyImageAttributeResponseType();
		
		param1.set_return( engineResponse );
		param1.setRequestId( UUID.randomUUID().toString());
        response.setModifyImageAttributeResponse( param1 );
		return response;
	}
	
	public static ResetImageAttributeResponse toResetImageAttributeResponse( boolean engineResponse ) {
		ResetImageAttributeResponse response = new ResetImageAttributeResponse();
		ResetImageAttributeResponseType param1 = new ResetImageAttributeResponseType();
		
		param1.set_return( engineResponse );
		param1.setRequestId( UUID.randomUUID().toString());
        response.setResetImageAttributeResponse( param1 );
		return response;		
	}
	
	public static DescribeImagesResponse toDescribeImagesResponse(EC2DescribeImagesResponse engineResponse ) {
		DescribeImagesResponse response = new DescribeImagesResponse();
		DescribeImagesResponseType param1 = new DescribeImagesResponseType();
		DescribeImagesResponseInfoType param2 = new DescribeImagesResponseInfoType();
		
		EC2Image[] images = engineResponse.getImageSet();
 	    for( int i=0; i < images.length; i++ ) {
 	    	String accountName = images[i].getAccountName();
			String domainId = images[i].getDomainId();
			String ownerId = domainId + ":" + accountName;
			
		    DescribeImagesResponseItemType param3 = new DescribeImagesResponseItemType();
		    param3.setImageId( images[i].getId());
		    param3.setImageLocation( "" );
		    param3.setImageState( (images[i].getIsReady() ? "available" : "unavailable" ));
		    param3.setImageOwnerId(ownerId);    
		    param3.setIsPublic( images[i].getIsPublic());

		    ProductCodesSetType param4 = new ProductCodesSetType();
	        ProductCodesSetItemType param5 = new ProductCodesSetItemType();
	        param5.setProductCode( "" );
            param4.addItem( param5 );		    
		    param3.setProductCodes( param4 );
		    
		    String description = images[i].getDescription();
		    param3.setDescription( (null == description ? "" : description));
		    
		         if (null == description) param3.setArchitecture( "" );
			else if (-1 != description.indexOf( "x86_64" )) param3.setArchitecture( "x86_64" );
			else if (-1 != description.indexOf( "i386"   )) param3.setArchitecture( "i386" );
			else param3.setArchitecture( "" );
		         
			param3.setImageType( "machine" );
		    param3.setKernelId( "" );
		    param3.setRamdiskId( "" );
		    param3.setPlatform( "" );
		    
		    StateReasonType param6 = new StateReasonType();
	        param6.setCode( "" );
	        param6.setMessage( "" );
            param3.setStateReason( param6 );
            
		    param3.setImageOwnerAlias( "" );
		    param3.setName( images[i].getName());
		    param3.setRootDeviceType( "" );
		    param3.setRootDeviceName( "" );
		    
		    BlockDeviceMappingType param7 = new BlockDeviceMappingType();
		    BlockDeviceMappingItemType param8 = new BlockDeviceMappingItemType();
		    BlockDeviceMappingItemTypeChoice_type0 param9 = new BlockDeviceMappingItemTypeChoice_type0();
		    param8.setDeviceName( "" );
		    param9.setVirtualName( "" );
		    EbsBlockDeviceType param10 = new EbsBlockDeviceType();
		    param10.setSnapshotId( "" );
		    param10.setVolumeSize( 0 );
		    param10.setDeleteOnTermination( false );
		    param9.setEbs( param10 );
		    param8.setBlockDeviceMappingItemTypeChoice_type0( param9 );
            param7.addItem( param8 );

            param3.setBlockDeviceMapping( param7 );
		    param2.addItem( param3 );
		}

		param1.setImagesSet( param2 );
		param1.setRequestId( UUID.randomUUID().toString());
		response.setDescribeImagesResponse( param1 );
		return response;
	}
	
	public static CreateImageResponse toCreateImageResponse(EC2CreateImageResponse engineResponse) {
		CreateImageResponse response = new CreateImageResponse();
		CreateImageResponseType param1 = new CreateImageResponseType();
       
		param1.setImageId( engineResponse.getId());
		param1.setRequestId( UUID.randomUUID().toString());
		response.setCreateImageResponse( param1 );
		return response;
	}
	
	public static RegisterImageResponse toRegisterImageResponse(EC2CreateImageResponse engineResponse) {
		RegisterImageResponse response = new RegisterImageResponse();
		RegisterImageResponseType param1 = new RegisterImageResponseType();

		param1.setImageId( engineResponse.getId());
		param1.setRequestId( UUID.randomUUID().toString());
		response.setRegisterImageResponse( param1 );
		return response;
	}
	
	public static DeregisterImageResponse toDeregisterImageResponse( boolean engineResponse) {
		DeregisterImageResponse response = new DeregisterImageResponse();
		DeregisterImageResponseType param1 = new DeregisterImageResponseType();
		
		param1.set_return( engineResponse );
		param1.setRequestId( UUID.randomUUID().toString());
        response.setDeregisterImageResponse( param1 );
		return response;
	}

	
	private EC2VolumeFilterSet toVolumeFilterSet( FilterSetType fst, String[] timeStrs )
	{
		EC2VolumeFilterSet vfs = new EC2VolumeFilterSet();
		boolean timeFilter = false;
		
		FilterType[] items = fst.getItem();
		if (null != items) 
		{
			// -> each filter can have one or more values associated with it
			for( int j=0; j < items.length; j++ )
			{
				EC2Filter oneFilter = new EC2Filter();
				String filterName = items[j].getName();
				oneFilter.setName( filterName );
				
				// -> is the filter one of the xsd:dateTime filters?
				timeFilter = false;
				for( int m=0; m < timeStrs.length; m++ )
				{
					 timeFilter = filterName.equalsIgnoreCase( timeStrs[m] );
					 if (timeFilter) break;
				}
				
				ValueSetType vst = items[j].getValueSet();
				ValueType[] valueItems = vst.getItem();
				for( int k=0; k < valueItems.length; k++ ) 
				{
					// -> time values are not encoded as regexes
					if ( timeFilter )
					     oneFilter.addValue( valueItems[k].getValue());
					else oneFilter.addValueEncoded( valueItems[k].getValue());
				}
				vfs.addFilter( oneFilter );
			}
		}		
		return vfs;
	}

	
	private EC2SnapshotFilterSet toSnapshotFilterSet( FilterSetType fst, String[] timeStrs )
	{
		EC2SnapshotFilterSet vfs = new EC2SnapshotFilterSet();
		boolean timeFilter = false;
		
		FilterType[] items = fst.getItem();
		if (null != items) 
		{
			// -> each filter can have one or more values associated with it
			for( int j=0; j < items.length; j++ )
			{
				EC2Filter oneFilter = new EC2Filter();
				String filterName = items[j].getName();
				oneFilter.setName( filterName );
				
				// -> is the filter one of the xsd:dateTime filters?
				timeFilter = false;
				for( int m=0; m < timeStrs.length; m++ )
				{
					 timeFilter = filterName.equalsIgnoreCase( timeStrs[m] );
					 if (timeFilter) break;
				}
				
				ValueSetType vst = items[j].getValueSet();
				ValueType[] valueItems = vst.getItem();
				for( int k=0; k < valueItems.length; k++ ) 
				{
					// -> time values are not encoded as regexes
					if ( timeFilter )
					     oneFilter.addValue( valueItems[k].getValue());
					else oneFilter.addValueEncoded( valueItems[k].getValue());
				}
				vfs.addFilter( oneFilter );
			}
		}		
		return vfs;
	}

	
	// TODO make these filter set functions use generics 
	private EC2GroupFilterSet toGroupFilterSet( FilterSetType fst )
	{
		EC2GroupFilterSet gfs = new EC2GroupFilterSet();
		
		FilterType[] items = fst.getItem();
		if (null != items) 
		{
			// -> each filter can have one or more values associated with it
			for( int j=0; j < items.length; j++ )
			{
				EC2Filter oneFilter = new EC2Filter();
				String filterName = items[j].getName();
				oneFilter.setName( filterName );
				
				ValueSetType vst = items[j].getValueSet();
				ValueType[] valueItems = vst.getItem();
				for( int k=0; k < valueItems.length; k++ ) 
				{
					oneFilter.addValueEncoded( valueItems[k].getValue());
				}
				gfs.addFilter( oneFilter );
			}
		}		
		return gfs;
	}

	
	private EC2InstanceFilterSet toInstanceFilterSet( FilterSetType fst )
	{
		EC2InstanceFilterSet ifs = new EC2InstanceFilterSet();
		
		FilterType[] items = fst.getItem();
		if (null != items) 
		{
			// -> each filter can have one or more values associated with it
			for( int j=0; j < items.length; j++ )
			{
				EC2Filter oneFilter = new EC2Filter();
				String filterName = items[j].getName();
				oneFilter.setName( filterName );
				
				ValueSetType vst = items[j].getValueSet();
				ValueType[] valueItems = vst.getItem();
				for( int k=0; k < valueItems.length; k++ ) 
				{
					oneFilter.addValueEncoded( valueItems[k].getValue());
				}
				ifs.addFilter( oneFilter );
			}
		}		
		return ifs;
	}

	
	public static DescribeVolumesResponse toDescribeVolumesResponse( EC2DescribeVolumesResponse engineResponse ) 
	{
	    DescribeVolumesResponse      response = new DescribeVolumesResponse();
	    DescribeVolumesResponseType    param1 = new DescribeVolumesResponseType();
	    DescribeVolumesSetResponseType param2 = new DescribeVolumesSetResponseType();
        
		EC2Volume[] volumes = engineResponse.getVolumeSet();
		for( int i=0; i < volumes.length; i++ ) 
		{
			DescribeVolumesSetItemResponseType param3 = new DescribeVolumesSetItemResponseType();
	        param3.setVolumeId( volumes[i].getId());
	        
	        Integer volSize = new Integer( volumes[i].getSize());
	        param3.setSize( volSize.toString());  
	        String snapId = volumes[i].getSnapShotId();
	        param3.setSnapshotId((null == snapId ? "" : snapId));
	        param3.setAvailabilityZone( volumes[i].getZoneName());
	        param3.setStatus( volumes[i].getState());
	        
        	// -> CloudStack seems to have issues with timestamp formats so just in case
	        Calendar cal = volumes[i].getCreated();
	        if ( null == cal ) 
	        {
	        	 cal = Calendar.getInstance();
	        	 cal.set( 1970, 1, 1 );
	        }
	        param3.setCreateTime( cal );
	        
	        AttachmentSetResponseType param4 = new AttachmentSetResponseType();
	        if (null != volumes[i].getInstanceId()) 
	        {
	        	AttachmentSetItemResponseType param5 = new AttachmentSetItemResponseType();
	        	param5.setVolumeId( volumes[i].getId());
	        	param5.setInstanceId( volumes[i].getInstanceId());
	        	String devicePath = engine.cloudDeviceIdToDevicePath( volumes[i].getHypervisor(), volumes[i].getDeviceId());
	        	param5.setDevice( devicePath );
	        	param5.setStatus( toVolumeAttachmentState( volumes[i].getInstanceId(), volumes[i].getVMState()));
	        	param5.setAttachTime( cal );  
	        	param5.setDeleteOnTermination( false );
                param4.addItem( param5 );
            }
	        
            param3.setAttachmentSet( param4 );
            
            // -> try to generate an empty tag does not seem to work
            ResourceTagSetType param6 = new ResourceTagSetType();
            ResourceTagSetItemType param7 = new ResourceTagSetItemType();
            param7.setKey("");
            param7.setValue("");
            param6.addItem( param7 );
            param3.setTagSet( param6 );          
            param2.addItem( param3 );
        }
	    param1.setVolumeSet( param2 );
	    param1.setRequestId( UUID.randomUUID().toString());
	    response.setDescribeVolumesResponse( param1 );
	    return response;
	}
	
	
	public static DescribeInstanceAttributeResponse toDescribeInstanceAttributeResponse(EC2DescribeInstancesResponse engineResponse) {
      	DescribeInstanceAttributeResponse response = new DescribeInstanceAttributeResponse();
      	DescribeInstanceAttributeResponseType param1 = new DescribeInstanceAttributeResponseType();

      	EC2Instance[] instanceSet = engineResponse.getInstanceSet();
      	if (0 < instanceSet.length) {
      		DescribeInstanceAttributeResponseTypeChoice_type0 param2 = new DescribeInstanceAttributeResponseTypeChoice_type0();
      		NullableAttributeValueType param3 = new NullableAttributeValueType();
      		param3.setValue( instanceSet[0].getServiceOffering());
      		param2.setInstanceType( param3 );
            param1.setDescribeInstanceAttributeResponseTypeChoice_type0( param2 );
      		param1.setInstanceId( instanceSet[0].getId());
      	}
	    param1.setRequestId( UUID.randomUUID().toString());
        response.setDescribeInstanceAttributeResponse( param1 );
		return response;
	}

	
	public static DescribeInstancesResponse toDescribeInstancesResponse(EC2DescribeInstancesResponse engineResponse, EC2Engine engine) 
	{
	    DescribeInstancesResponse response = new DescribeInstancesResponse();
	    DescribeInstancesResponseType param1 = new DescribeInstancesResponseType();
	    ReservationSetType param2 = new ReservationSetType();

		EC2Instance[] instances = engineResponse.getInstanceSet();
		for( int i=0; i < instances.length; i++ ) 
		{
			String accountName = instances[i].getAccountName();
			String domainId = instances[i].getDomainId();
			String ownerId = domainId + ":" + accountName;
		
			ReservationInfoType param3 = new ReservationInfoType();
	        param3.setReservationId( instances[i].getId());   // -> an id we could track down if needed
	        param3.setOwnerId(ownerId);
	        param3.setRequesterId( "" );
	        
			GroupSetType  param4 = new GroupSetType();
			GroupItemType param5 = new GroupItemType();
			param5.setGroupId( (null == instances[i].getGroup() ? "" : instances[i].getGroup()));
			param4.addItem( param5 );
	        param3.setGroupSet( param4 );
	        
	        RunningInstancesSetType  param6 = new RunningInstancesSetType();
	        RunningInstancesItemType param7 = new RunningInstancesItemType();
	        param7.setInstanceId( instances[i].getId());
	        param7.setImageId( instances[i].getTemplateId());
	        
	        InstanceStateType param8 = new InstanceStateType();
	        param8.setCode( toAmazonCode( instances[i].getState()));
	        param8.setName( toAmazonStateName( instances[i].getState()));
	        param7.setInstanceState( param8 );
	        
	        param7.setPrivateDnsName( "" );
	        param7.setDnsName( "" );
	        param7.setReason( "" );
	        param7.setKeyName( "" );
	        param7.setAmiLaunchIndex( "" );
	        param7.setInstanceType( instances[i].getServiceOffering());
	        
	        ProductCodesSetType param9 = new ProductCodesSetType();
	        ProductCodesSetItemType param10 = new ProductCodesSetItemType();
	        param10.setProductCode( "" );
            param9.addItem( param10 );
	        param7.setProductCodes( param9 );
	        
	        Calendar cal = instances[i].getCreated();
	        if ( null == cal ) {
	        	 cal = Calendar.getInstance();
	        	 cal.set( 1970, 1, 1 );
	        }
	        param7.setLaunchTime( cal );
	        
	        PlacementResponseType param11 = new PlacementResponseType();
	        param11.setAvailabilityZone( instances[i].getZoneName());
	        param11.setGroupName( "" );
	        param7.setPlacement( param11 );
	        param7.setKernelId( "" );
	        param7.setRamdiskId( "" );
	        param7.setPlatform( "" );
	        
	        InstanceMonitoringStateType param12 = new InstanceMonitoringStateType();
	        param12.setState( "" );
            param7.setMonitoring( param12 );
            param7.setSubnetId( "" );
            param7.setVpcId( "" );
            String ipAddr = instances[i].getPrivateIpAddress();
            param7.setPrivateIpAddress((null != ipAddr ? ipAddr : ""));
	        param7.setIpAddress( instances[i].getIpAddress());
	        
	        StateReasonType param13 = new StateReasonType();
	        param13.setCode( "" );
	        param13.setMessage( "" );
            param7.setStateReason( param13 );
            param7.setArchitecture( "" );
            param7.setRootDeviceType( "" );
        	String devicePath = engine.cloudDeviceIdToDevicePath( instances[i].getHypervisor(), instances[i].getRootDeviceId());
            param7.setRootDeviceName( devicePath );
            
            InstanceBlockDeviceMappingResponseType param14 = new InstanceBlockDeviceMappingResponseType();
            InstanceBlockDeviceMappingResponseItemType param15 = new InstanceBlockDeviceMappingResponseItemType();
            InstanceBlockDeviceMappingResponseItemTypeChoice_type0 param16 = new InstanceBlockDeviceMappingResponseItemTypeChoice_type0();
            param15.setDeviceName( "" );        
            EbsInstanceBlockDeviceMappingResponseType param17 = new EbsInstanceBlockDeviceMappingResponseType();
            param17.setVolumeId( "" );
            param17.setStatus( "" );           
            param17.setAttachTime( cal );
            
            param17.setDeleteOnTermination( true );
            param16.setEbs( param17 );
            param15.setInstanceBlockDeviceMappingResponseItemTypeChoice_type0( param16 );
            param14.addItem( param15 );
            param7.setBlockDeviceMapping( param14 );
            param7.setInstanceLifecycle( "" );
            param7.setSpotInstanceRequestId( "" );

	        param6.addItem( param7 );
	        param3.setInstancesSet( param6 );
	        param2.addItem( param3 );
		}
	    param1.setReservationSet( param2 );
	    param1.setRequestId( UUID.randomUUID().toString());
	    response.setDescribeInstancesResponse( param1 );
		return response;
	}

	
    public static DescribeAddressesResponse toDescribeAddressesResponse(Map[] addresses) {
        final DescribeAddressesResponseItemType[] items = new DescribeAddressesResponseItemType[addresses.length];

        for (int i = 0; i < addresses.length; i++) {
            Map a = addresses[i];

            items[i] = new DescribeAddressesResponseItemType();
            items[i].setPublicIp(a.get("ipaddress").toString());
            if (a.containsKey("virtualmachinename")) {
                items[i].setInstanceId(a.get("virtualmachinename").toString());
            }
        }

        return new DescribeAddressesResponse() {{
            setDescribeAddressesResponse(new DescribeAddressesResponseType() {{
                setRequestId(UUID.randomUUID().toString());
                setAddressesSet(new DescribeAddressesResponseInfoType() {{
                    setItem(items);
                }});
            }});
        }};
    }

    public static AllocateAddressResponse toAllocateAddressResponse(final String publicIp) {

        return new AllocateAddressResponse() {{
            setAllocateAddressResponse(new AllocateAddressResponseType() {{
                setRequestId(UUID.randomUUID().toString());
                setPublicIp(publicIp);
            }});
        }};
    }

    public static ReleaseAddressResponse toReleaseAddressResponse(final boolean result) {

        return new ReleaseAddressResponse() {{
            setReleaseAddressResponse(new ReleaseAddressResponseType() {{
                setRequestId(UUID.randomUUID().toString());
                set_return(result);
            }});
        }};
    }

    public static AssociateAddressResponse toAssociateAddressResponse(final boolean result) {

        return new AssociateAddressResponse() {{
            setAssociateAddressResponse(new AssociateAddressResponseType() {{
                setRequestId(UUID.randomUUID().toString());
                set_return(result);
            }});
        }};
    }

    public static DisassociateAddressResponse toDisassociateAddressResponse(final boolean result) {

        return new DisassociateAddressResponse() {{
            setDisassociateAddressResponse(new DisassociateAddressResponseType() {{
                setRequestId(UUID.randomUUID().toString());
                set_return(result);
            }});
        }};
    }

	/**
	 * Map our cloud state values into what Amazon defines.
	 * Where are the values that can be returned by our cloud api defined?
	 * 
	 * @param cloudState
	 * @return 
	 */
	public static int toAmazonCode( String cloudState )
	{
		if (null == cloudState) return 48;
		
		     if (cloudState.equalsIgnoreCase( "Destroyed" )) return 48;
		else if (cloudState.equalsIgnoreCase( "Stopped"   )) return 80;
		else if (cloudState.equalsIgnoreCase( "Running"   )) return 16;
		else if (cloudState.equalsIgnoreCase( "Starting"  )) return 0;
		else if (cloudState.equalsIgnoreCase( "Stopping"  )) return 64;
		else if (cloudState.equalsIgnoreCase( "Error"     )) return 1;
		else return 16;
	}
	
	public static String toAmazonStateName( String cloudState )
	{
		if (null == cloudState) return new String( "terminated" );
		
		     if (cloudState.equalsIgnoreCase( "Destroyed" )) return new String( "terminated" );
		else if (cloudState.equalsIgnoreCase( "Stopped"   )) return new String( "stopped" );
		else if (cloudState.equalsIgnoreCase( "Running"   )) return new String( "running" );
		else if (cloudState.equalsIgnoreCase( "Starting"  )) return new String( "pending" );
		else if (cloudState.equalsIgnoreCase( "Stopping"  )) return new String( "stopping" );
		else if (cloudState.equalsIgnoreCase( "Error"     )) return new String( "error" );
		else return new String( "running" );
	}
	
	/**
	 * We assume a state for the volume based on what its associated VM is doing.
	 * 
	 * @param vmId
	 * @param vmState
	 * @return
	 */
	public static String toVolumeAttachmentState( String instanceId, String vmState )
	{
		if (null == instanceId || null == vmState) return "detached";
		
		     if (vmState.equalsIgnoreCase( "Destroyed" )) return "detached";
		else if (vmState.equalsIgnoreCase( "Stopped"   )) return "attached";
		else if (vmState.equalsIgnoreCase( "Running"   )) return "attached";
		else if (vmState.equalsIgnoreCase( "Starting"  )) return "attaching";
		else if (vmState.equalsIgnoreCase( "Stopping"  )) return "attached";
		else if (vmState.equalsIgnoreCase( "Error"     )) return "detached";
		else return "detached";
	}
	
	public static StopInstancesResponse toStopInstancesResponse(EC2StopInstancesResponse engineResponse) {
	    StopInstancesResponse response = new StopInstancesResponse();
	    StopInstancesResponseType param1 = new StopInstancesResponseType();
	    InstanceStateChangeSetType param2 = new InstanceStateChangeSetType();

		EC2Instance[] instances = engineResponse.getInstanceSet();
		for( int i=0; i < instances.length; i++ ) {
			InstanceStateChangeType param3 = new InstanceStateChangeType();
			param3.setInstanceId( instances[i].getId());
			
			InstanceStateType param4 = new InstanceStateType();
	        param4.setCode( toAmazonCode( instances[i].getState()));
	        param4.setName( toAmazonStateName( instances[i].getState()));
			param3.setCurrentState( param4 );

			InstanceStateType param5 = new InstanceStateType();
	        param5.setCode( toAmazonCode( instances[i].getPreviousState() ));
	        param5.setName( toAmazonStateName( instances[i].getPreviousState() ));
			param3.setPreviousState( param5 );
			
			param2.addItem( param3 );
		}
		
	    param1.setRequestId( UUID.randomUUID().toString());
        param1.setInstancesSet( param2 );
	    response.setStopInstancesResponse( param1 );
		return response;
	}
	
	public static StartInstancesResponse toStartInstancesResponse(EC2StartInstancesResponse engineResponse) {
	    StartInstancesResponse response = new StartInstancesResponse();
	    StartInstancesResponseType param1 = new StartInstancesResponseType();
	    InstanceStateChangeSetType param2 = new InstanceStateChangeSetType();

		EC2Instance[] instances = engineResponse.getInstanceSet();
		for( int i=0; i < instances.length; i++ ) {
			InstanceStateChangeType param3 = new InstanceStateChangeType();
			param3.setInstanceId( instances[i].getId());
			
			InstanceStateType param4 = new InstanceStateType();
	        param4.setCode( toAmazonCode( instances[i].getState()));
	        param4.setName( toAmazonStateName( instances[i].getState()));
			param3.setCurrentState( param4 );

			InstanceStateType param5 = new InstanceStateType();
	        param5.setCode( toAmazonCode( instances[i].getPreviousState() ));
	        param5.setName( toAmazonStateName( instances[i].getPreviousState() ));
			param3.setPreviousState( param5 );
			
			param2.addItem( param3 );
		}
		
	    param1.setRequestId( UUID.randomUUID().toString());
        param1.setInstancesSet( param2 );
	    response.setStartInstancesResponse( param1 );
		return response;
	}
	
	public static TerminateInstancesResponse toTermInstancesResponse(EC2StopInstancesResponse engineResponse) {
		TerminateInstancesResponse response = new TerminateInstancesResponse();
		TerminateInstancesResponseType param1 = new TerminateInstancesResponseType();
	    InstanceStateChangeSetType param2 = new InstanceStateChangeSetType();

		EC2Instance[] instances = engineResponse.getInstanceSet();
		for( int i=0; i < instances.length; i++ ) {
			InstanceStateChangeType param3 = new InstanceStateChangeType();
			param3.setInstanceId( instances[i].getId());
			
			InstanceStateType param4 = new InstanceStateType();
	        param4.setCode( toAmazonCode( instances[i].getState()));
	        param4.setName( toAmazonStateName( instances[i].getState()));
			param3.setCurrentState( param4 );

			InstanceStateType param5 = new InstanceStateType();
	        param5.setCode( toAmazonCode( instances[i].getPreviousState() ));
	        param5.setName( toAmazonStateName( instances[i].getPreviousState() ));
			param3.setPreviousState( param5 );
			
			param2.addItem( param3 );
		}
		
	    param1.setRequestId( UUID.randomUUID().toString());
        param1.setInstancesSet( param2 );
	    response.setTerminateInstancesResponse( param1 );
		return response;
	}
	
	public static RebootInstancesResponse toRebootInstancesResponse(boolean engineResponse) {
	    RebootInstancesResponse response = new RebootInstancesResponse();
	    RebootInstancesResponseType param1 = new RebootInstancesResponseType();

	    param1.setRequestId( UUID.randomUUID().toString());
	    param1.set_return( engineResponse );
	    response.setRebootInstancesResponse( param1 );
		return response;
	}

	public static RunInstancesResponse toRunInstancesResponse(EC2RunInstancesResponse engineResponse, EC2Engine engine ) {
	    RunInstancesResponse response = new RunInstancesResponse();
	    RunInstancesResponseType param1 = new RunInstancesResponseType();

	    param1.setReservationId( "" );
	    
		GroupSetType  param2 = new GroupSetType();
		GroupItemType param3 = new GroupItemType();
		param3.setGroupId( "" );
		param2.addItem( param3 );
	    param1.setGroupSet( param2 );
	    
	    RunningInstancesSetType param6 = new RunningInstancesSetType();
		EC2Instance[] instances = engineResponse.getInstanceSet();
		for( int i=0; i < instances.length; i++ ) {
	        RunningInstancesItemType param7 = new RunningInstancesItemType();
	        param7.setInstanceId( instances[i].getId());
	        param7.setImageId( instances[i].getTemplateId());
	        
	        String accountName = instances[i].getAccountName();
			String domainId = instances[i].getDomainId();
			String ownerId = domainId + ":" + accountName;
		
	        param1.setOwnerId(ownerId);
			
	        InstanceStateType param8 = new InstanceStateType();
	        param8.setCode( toAmazonCode( instances[i].getState()));
	        param8.setName( toAmazonStateName( instances[i].getState()));
	        param7.setInstanceState( param8 );
	        
	        param7.setPrivateDnsName( "" );
	        param7.setDnsName( "" );
	        param7.setReason( "" );
	        param7.setKeyName( "" );
	        param7.setAmiLaunchIndex( "" );
	        
	        ProductCodesSetType param9 = new ProductCodesSetType();
	        ProductCodesSetItemType param10 = new ProductCodesSetItemType();
	        param10.setProductCode( "" );
            param9.addItem( param10 );
	        param7.setProductCodes( param9 );
	        
	        param7.setInstanceType( instances[i].getServiceOffering());
        	// -> CloudStack seems to have issues with timestamp formats so just in case
	        Calendar cal = instances[i].getCreated();
	        if ( null == cal ) {
	        	 cal = Calendar.getInstance();
	        	 cal.set( 1970, 1, 1 );
	        }
	        param7.setLaunchTime( cal );

	        PlacementResponseType param11 = new PlacementResponseType();
	        param11.setAvailabilityZone( instances[i].getZoneName());
	        param7.setPlacement( param11 );
	        
	        param7.setKernelId( "" );
	        param7.setRamdiskId( "" );
	        param7.setPlatform( "" );
	        
	        InstanceMonitoringStateType param12 = new InstanceMonitoringStateType();
	        param12.setState( "" );
            param7.setMonitoring( param12 );
            param7.setSubnetId( "" );
            param7.setVpcId( "" );
            String ipAddr = instances[i].getIpAddress();
            param7.setPrivateIpAddress((null != ipAddr ? ipAddr : ""));
	        param7.setIpAddress( instances[i].getIpAddress());

	        StateReasonType param13 = new StateReasonType();
	        param13.setCode( "" );
	        param13.setMessage( "" );
            param7.setStateReason( param13 );
            param7.setArchitecture( "" );
            param7.setRootDeviceType( "" );
            param7.setRootDeviceName( "" );
            
            InstanceBlockDeviceMappingResponseType param14 = new InstanceBlockDeviceMappingResponseType();
            InstanceBlockDeviceMappingResponseItemType param15 = new InstanceBlockDeviceMappingResponseItemType();
            InstanceBlockDeviceMappingResponseItemTypeChoice_type0 param16 = new InstanceBlockDeviceMappingResponseItemTypeChoice_type0();
            param15.setDeviceName( "" );           
            EbsInstanceBlockDeviceMappingResponseType param17 = new EbsInstanceBlockDeviceMappingResponseType();
            param17.setVolumeId( "" );
            param17.setStatus( "" );
            param17.setAttachTime( cal );
            param17.setDeleteOnTermination( true );
            param16.setEbs( param17 );
            param15.setInstanceBlockDeviceMappingResponseItemTypeChoice_type0( param16 );
            param14.addItem( param15 );
            param7.setBlockDeviceMapping( param14 );
            
            param7.setInstanceLifecycle( "" );
            param7.setSpotInstanceRequestId( "" );
            param7.setVirtualizationType( "" );
            param7.setClientToken( "" );
            
            ResourceTagSetType param18 = new ResourceTagSetType();
            ResourceTagSetItemType param19 = new ResourceTagSetItemType();
            param19.setKey("");
            param19.setValue("");
            param18.addItem( param19 );
            param7.setTagSet( param18 );          
            
            String hypervisor = instances[i].getHypervisor();
            param7.setHypervisor((null != hypervisor ? hypervisor : ""));
	        param6.addItem( param7 );
		}
		param1.setInstancesSet( param6 );
		param1.setRequesterId( "" );
	    
	    param1.setRequestId( UUID.randomUUID().toString());
	    response.setRunInstancesResponse( param1 );
		return response;
	}

	public static DescribeAvailabilityZonesResponse toDescribeAvailabilityZonesResponse(EC2DescribeAvailabilityZonesResponse engineResponse) {
		DescribeAvailabilityZonesResponse response = new DescribeAvailabilityZonesResponse();
		DescribeAvailabilityZonesResponseType param1 = new DescribeAvailabilityZonesResponseType();
        AvailabilityZoneSetType param2 = new AvailabilityZoneSetType();
        
		String[] zones = engineResponse.getZoneSet();
		for( int i=0; i < zones.length; i++ ) {
            AvailabilityZoneItemType param3 = new AvailabilityZoneItemType(); 
            AvailabilityZoneMessageSetType param4 = new AvailabilityZoneMessageSetType();
            param3.setZoneName( zones[i] );
            param3.setZoneState( "available" );
            param3.setRegionName( "" );
            param3.setMessageSet( param4 );
            param2.addItem( param3 );
		}

	    param1.setRequestId( UUID.randomUUID().toString());
        param1.setAvailabilityZoneInfo( param2 );
	    response.setDescribeAvailabilityZonesResponse( param1 );
		return response;
	}
	
	public static AttachVolumeResponse toAttachVolumeResponse(EC2Volume engineResponse) {
		AttachVolumeResponse response = new AttachVolumeResponse();
		AttachVolumeResponseType param1 = new AttachVolumeResponseType();
	    Calendar cal = Calendar.getInstance();
	    cal.set( 1970, 1, 1 );   // return one value, Unix Epoch, what else can we return? 
		
	    // -> if the instanceId was not given in the request then we have no way to get it
		param1.setVolumeId( engineResponse.getId());
		param1.setInstanceId( engineResponse.getInstanceId());
		param1.setDevice( engineResponse.getDevice());
		if ( null != engineResponse.getState())
		     param1.setStatus( engineResponse.getState());
		else param1.setStatus( "" );  // ToDo - throw an Soap Fault 
		
		param1.setAttachTime( cal );
			
		param1.setRequestId( UUID.randomUUID().toString());
        response.setAttachVolumeResponse( param1 );
		return response;
	}

	public static DetachVolumeResponse toDetachVolumeResponse(EC2Volume engineResponse) {
		DetachVolumeResponse response = new DetachVolumeResponse();
		DetachVolumeResponseType param1 = new DetachVolumeResponseType();
	    Calendar cal = Calendar.getInstance();
	    cal.set( 1970, 1, 1 );   // return one value, Unix Epoch, what else can we return? 
		
		param1.setVolumeId( engineResponse.getId());
		param1.setInstanceId( (null == engineResponse.getInstanceId() ? "" : engineResponse.getInstanceId()));
		param1.setDevice( (null == engineResponse.getDevice() ? "" : engineResponse.getDevice()));
		if ( null != engineResponse.getState())
		     param1.setStatus( engineResponse.getState());
		else param1.setStatus( "" );  // ToDo - throw an Soap Fault 
		
		param1.setAttachTime( cal );
			
		param1.setRequestId( UUID.randomUUID().toString());
        response.setDetachVolumeResponse( param1 );
		return response;
	}
	
	public static CreateVolumeResponse toCreateVolumeResponse(EC2Volume engineResponse) {
		CreateVolumeResponse response = new CreateVolumeResponse();
		CreateVolumeResponseType param1 = new CreateVolumeResponseType();
		
		param1.setVolumeId( engineResponse.getId());
        Integer volSize = new Integer( engineResponse.getSize());
        param1.setSize( volSize.toString());  
        param1.setSnapshotId( "" );
        param1.setAvailabilityZone( engineResponse.getZoneName());
		if ( null != engineResponse.getState())
		     param1.setStatus( engineResponse.getState());
		else param1.setStatus( "" );  // ToDo - throw an Soap Fault 
		
       	// -> CloudStack seems to have issues with timestamp formats so just in case
        Calendar cal = engineResponse.getCreated();
        if ( null == cal ) {
        	 cal = Calendar.getInstance();
        	 cal.set( 1970, 1, 1 );
        }
		param1.setCreateTime( cal );

		param1.setRequestId( UUID.randomUUID().toString());
        response.setCreateVolumeResponse( param1 );
		return response;
	}

	public static DeleteVolumeResponse toDeleteVolumeResponse(EC2Volume engineResponse) {
		DeleteVolumeResponse response = new DeleteVolumeResponse();
		DeleteVolumeResponseType param1 = new DeleteVolumeResponseType();
		
		if ( null != engineResponse.getState())
			 param1.set_return( true  );
		else param1.set_return( false );  // ToDo - supposed to return an error
	
		param1.setRequestId( UUID.randomUUID().toString());
        response.setDeleteVolumeResponse( param1 );
		return response;
	}
	
	public static DescribeSnapshotsResponse toDescribeSnapshotsResponse(EC2DescribeSnapshotsResponse engineResponse) {
	    DescribeSnapshotsResponse response = new DescribeSnapshotsResponse();
	    DescribeSnapshotsResponseType param1 = new DescribeSnapshotsResponseType();
	    DescribeSnapshotsSetResponseType param2 = new DescribeSnapshotsSetResponseType();
        
	    EC2Snapshot[] snapshots = engineResponse.getSnapshotSet();
	    for( int i=0; i < snapshots.length; i++ ) {
	         DescribeSnapshotsSetItemResponseType param3 = new DescribeSnapshotsSetItemResponseType();
	         param3.setSnapshotId( snapshots[i].getId());
	         param3.setVolumeId( snapshots[i].getVolumeId());
	         param3.setStatus( snapshots[i].getState());
	         
	         String accountName = snapshots[i].getAccountName();
	         String domainId = snapshots[i].getDomainId();
				String ownerId = domainId + ":" + accountName;
	         
	         // -> CloudStack seems to have issues with timestamp formats so just in case
		     Calendar cal = snapshots[i].getCreated();
		     if ( null == cal ) {
		       	  cal = Calendar.getInstance();
		       	  cal.set( 1970, 1, 1 );
		     }
	         param3.setStartTime( cal );
	         
	         param3.setProgress( "" );
	         param3.setOwnerId(ownerId);
	         Integer volSize = new Integer( snapshots[i].getVolumeSize());
	         param3.setVolumeSize( volSize.toString());
	         param3.setDescription( snapshots[i].getName());
	         param3.setOwnerAlias( "" );
	         
	         ResourceTagSetType param18 = new ResourceTagSetType();
	         ResourceTagSetItemType param19 = new ResourceTagSetItemType();
	         param19.setKey("");
	         param19.setValue("");
	         param18.addItem( param19 );
	         param3.setTagSet( param18 );          
             param2.addItem( param3 );
	    }
	    
	    param1.setSnapshotSet( param2 );
	    param1.setRequestId( UUID.randomUUID().toString());
	    response.setDescribeSnapshotsResponse( param1 );
	    return response;
	}
	
	public static DeleteSnapshotResponse toDeleteSnapshotResponse( boolean engineResponse ) {
		DeleteSnapshotResponse response = new DeleteSnapshotResponse();
		DeleteSnapshotResponseType param1 = new DeleteSnapshotResponseType();
	
		param1.set_return( engineResponse );
	    param1.setRequestId( UUID.randomUUID().toString());
        response.setDeleteSnapshotResponse( param1 );
		return response;
	}
	
	public static CreateSnapshotResponse toCreateSnapshotResponse(EC2Snapshot engineResponse, EC2Engine engine ) {
		CreateSnapshotResponse response = new CreateSnapshotResponse();
		CreateSnapshotResponseType param1 = new CreateSnapshotResponseType();
		
		String accountName = engineResponse.getAccountName();
		String domainId = engineResponse.getDomainId();
		String ownerId = domainId + ":" + accountName;

		param1.setSnapshotId( engineResponse.getId());
		param1.setVolumeId( engineResponse.getVolumeId());
		param1.setStatus( "completed" );
		
       	// -> CloudStack seems to have issues with timestamp formats so just in case
        Calendar cal = engineResponse.getCreated();
        if ( null == cal ) {
        	 cal = Calendar.getInstance();
        	 cal.set( 1970, 1, 1 );
        }
		param1.setStartTime( cal );
		
		param1.setProgress( "100" );
		param1.setOwnerId(ownerId);
        Integer volSize = new Integer( engineResponse.getVolumeSize());
        param1.setVolumeSize( volSize.toString());
        param1.setDescription( engineResponse.getName());
	    param1.setRequestId( UUID.randomUUID().toString());
        response.setCreateSnapshotResponse( param1 );
		return response;
	}
	
	public static DescribeSecurityGroupsResponse toDescribeSecurityGroupsResponse(EC2DescribeSecurityGroupsResponse engineResponse ) {
		DescribeSecurityGroupsResponse response = new DescribeSecurityGroupsResponse();
		DescribeSecurityGroupsResponseType param1 = new DescribeSecurityGroupsResponseType();
		SecurityGroupSetType param2 = new SecurityGroupSetType();
		
		EC2SecurityGroup[] groups = engineResponse.getGroupSet();
	    for( int i=0; i < groups.length; i++ ) {
		     SecurityGroupItemType param3 = new SecurityGroupItemType();
		     String accountName = groups[i].getAccountName();
			 String domainId = groups[i].getDomainId();
			 String ownerId = domainId + ":" + accountName;
		     
		     param3.setOwnerId(ownerId);
		     param3.setGroupName( groups[i].getName());
		     String desc = groups[i].getDescription();
		     param3.setGroupDescription((null != desc ? desc : ""));
		     
		     IpPermissionSetType param4 = new IpPermissionSetType();
		     EC2IpPermission[] perms = groups[i].getIpPermissionSet();
		     for( int j=0; j < perms.length; j++ ) 
		     {
		    	IpPermissionType param5 = new IpPermissionType();
		    	param5.setIpProtocol( perms[j].getProtocol());
		    	param5.setFromPort( perms[j].getFromPort());
		    	param5.setToPort( perms[j].getToPort());
		    	
		    	// -> user groups
		    	EC2SecurityGroup[] userSet = perms[j].getUserSet();
		    	if ( null == userSet || 0 == userSet.length ) {
			    	 UserIdGroupPairSetType param8 = new UserIdGroupPairSetType();
			    	 param5.setGroups( param8 );	
		    	}
		    	else {
		    		 for( int h=0; h < userSet.length; h++ ) {
				    	 UserIdGroupPairSetType param8 = new UserIdGroupPairSetType();
				    	 UserIdGroupPairType param9 = new UserIdGroupPairType();
				    	 param9.setUserId( userSet[h].getAccount());
				    	 param9.setGroupName( userSet[h].getName());
				    	 param8.addItem( param9 );
				    	 param5.setGroups( param8 );			    			 
		    		 }
		    	}
		    	
		    	// -> or CIDR list
		    	String[] rangeSet = perms[j].getIpRangeSet();
		    	if ( null == rangeSet || 0 == rangeSet.length ) {
	    		     IpRangeSetType param6 = new IpRangeSetType();
 		    	     param5.setIpRanges( param6 );	
		    	}
		    	else {
		    	     for( int k=0; k < rangeSet.length; k++ ) {
		    		    IpRangeSetType  param6 = new IpRangeSetType();
		    		    IpRangeItemType param7 = new IpRangeItemType();
		    		    param7.setCidrIp( rangeSet[k] );
		    		    param6.addItem( param7 );
			    	    param5.setIpRanges( param6 );
		    	     }
		    	}
		        param4.addItem( param5 );
		     }
             param3.setIpPermissions( param4 );     	 
		     param2.addItem( param3 );
	    }
		
		param1.setSecurityGroupInfo( param2 );
	    param1.setRequestId( UUID.randomUUID().toString());
		response.setDescribeSecurityGroupsResponse( param1 );
		return response;
	}
	
	public static CreateSecurityGroupResponse toCreateSecurityGroupResponse( boolean success ) {
		CreateSecurityGroupResponse response = new CreateSecurityGroupResponse();
		CreateSecurityGroupResponseType param1 = new CreateSecurityGroupResponseType();
		
		param1.set_return( success );
		param1.setRequestId( UUID.randomUUID().toString());
		response.setCreateSecurityGroupResponse( param1 );
		return response;
	}
	
	public static DeleteSecurityGroupResponse toDeleteSecurityGroupResponse( boolean success ) {
		DeleteSecurityGroupResponse response = new DeleteSecurityGroupResponse();
		DeleteSecurityGroupResponseType param1 = new DeleteSecurityGroupResponseType();
		
		param1.set_return( success );
		param1.setRequestId( UUID.randomUUID().toString());
		response.setDeleteSecurityGroupResponse( param1 );
		return response;
	}
	
	public static AuthorizeSecurityGroupIngressResponse toAuthorizeSecurityGroupIngressResponse( boolean success ) {
		AuthorizeSecurityGroupIngressResponse response = new AuthorizeSecurityGroupIngressResponse();
		AuthorizeSecurityGroupIngressResponseType param1 = new AuthorizeSecurityGroupIngressResponseType();
	
		param1.set_return( success );
		param1.setRequestId( UUID.randomUUID().toString());
		response.setAuthorizeSecurityGroupIngressResponse( param1 );
        return response;
	}
	
	public static RevokeSecurityGroupIngressResponse toRevokeSecurityGroupIngressResponse( boolean success ) {
		RevokeSecurityGroupIngressResponse response = new RevokeSecurityGroupIngressResponse();
		RevokeSecurityGroupIngressResponseType param1 = new RevokeSecurityGroupIngressResponseType();
	
		param1.set_return( success );
		param1.setRequestId( UUID.randomUUID().toString());
		response.setRevokeSecurityGroupIngressResponse( param1 );
        return response;
	}
	
	public DescribeKeyPairsResponse describeKeyPairs(DescribeKeyPairs describeKeyPairs) {
		// TODO: Handle filters for key-name and fingerprint
		
		return toDescribeKeyPairs(engine.describeKeyPairs());
	}
	
	@SuppressWarnings("serial")
	public static DescribeKeyPairsResponse toDescribeKeyPairs(final List<EC2SSHKeyPair> keyList) {
		return new DescribeKeyPairsResponse() {{
			setDescribeKeyPairsResponse(new DescribeKeyPairsResponseType() {{ 
				setRequestId(UUID.randomUUID().toString());
				setKeySet(new DescribeKeyPairsResponseInfoType());
				if (keyList != null && keyList.size() > 0) {
					for (final EC2SSHKeyPair key : keyList) { 
						getKeySet().addItem(new DescribeKeyPairsResponseItemType() {{
							setKeyName(key.getKeyName());
							setKeyFingerprint(key.getFingerprint());
						}});
					}
				}
			}});
		}};
	}
	
	public ImportKeyPairResponse importKeyPair(ImportKeyPair importKeyPair) {
		String keyName = importKeyPair.getImportKeyPair().getKeyName();
		String publicKey = importKeyPair.getImportKeyPair().getPublicKeyMaterial();
        if (!publicKey.contains(" "))
             publicKey = new String(Base64.decodeBase64(publicKey.getBytes())); 
		
		return toImportKeyPair(engine.importKeyPair(keyName, publicKey));
	}
	
	@SuppressWarnings("serial")
	public static ImportKeyPairResponse toImportKeyPair(final EC2SSHKeyPair key) {
		return new ImportKeyPairResponse() {{
			setImportKeyPairResponse(new ImportKeyPairResponseType() {{
				setRequestId(UUID.randomUUID().toString());
				setKeyName(key.getKeyName());
				setKeyFingerprint(key.getFingerprint());
			}});
		}}; 
	}
	
	public CreateKeyPairResponse createKeyPair(CreateKeyPair createKeyPair) {
		return toCreateKeyPair(engine.createKeyPair(createKeyPair.getCreateKeyPair().getKeyName()));
	}
	
	@SuppressWarnings("serial")
	public static CreateKeyPairResponse toCreateKeyPair(final EC2SSHKeyPair key) {
		return new CreateKeyPairResponse() {{
			setCreateKeyPairResponse(new CreateKeyPairResponseType() {{
				setRequestId(UUID.randomUUID().toString());
				setKeyName(key.getKeyName());
				setKeyFingerprint(key.getFingerprint());
				setKeyMaterial(key.getPrivateKey());
			}});
		}};
	}
	
	public DeleteKeyPairResponse deleteKeyPair(DeleteKeyPair deleteKeyPair) {
		return toDeleteKeyPair(engine.deleteKeyPair(deleteKeyPair.getDeleteKeyPair().getKeyName()));
	}
	
	@SuppressWarnings("serial")
	public static DeleteKeyPairResponse toDeleteKeyPair(final boolean success) {
		return new DeleteKeyPairResponse() {{
			setDeleteKeyPairResponse(new DeleteKeyPairResponseType() {{
				setRequestId(UUID.randomUUID().toString());
				set_return(success);
			}});
		}};
	}
	
	public GetPasswordDataResponse getPasswordData(GetPasswordData getPasswordData) {
		return toGetPasswordData(engine.getPasswordData(getPasswordData.getGetPasswordData().getInstanceId()));
	}
	
	@SuppressWarnings("serial")
	public static GetPasswordDataResponse toGetPasswordData(final EC2PasswordData passwdData) {
		return new GetPasswordDataResponse() {{
			setGetPasswordDataResponse(new GetPasswordDataResponseType() {{
				setRequestId(UUID.randomUUID().toString());
				setTimestamp(Calendar.getInstance());
				setPasswordData(passwdData.getEncryptedPassword());
				setInstanceId(passwdData.getInstanceId());
			}});
		}};
	}

	
	
	
	// Actions not yet implemented:
	
	public ActivateLicenseResponse activateLicense(ActivateLicense activateLicense) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public AssociateDhcpOptionsResponse associateDhcpOptions(AssociateDhcpOptions associateDhcpOptions) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	};
	
	public AttachVpnGatewayResponse attachVpnGateway(AttachVpnGateway attachVpnGateway) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public BundleInstanceResponse bundleInstance(BundleInstance bundleInstance) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public CancelBundleTaskResponse cancelBundleTask(CancelBundleTask cancelBundleTask) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public CancelConversionTaskResponse cancelConversionTask(CancelConversionTask cancelConversionTask) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public CancelSpotInstanceRequestsResponse cancelSpotInstanceRequests(CancelSpotInstanceRequests cancelSpotInstanceRequests) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public ConfirmProductInstanceResponse confirmProductInstance(ConfirmProductInstance confirmProductInstance) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public CreateCustomerGatewayResponse createCustomerGateway(CreateCustomerGateway createCustomerGateway) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public CreateDhcpOptionsResponse createDhcpOptions(CreateDhcpOptions createDhcpOptions) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public CreatePlacementGroupResponse createPlacementGroup(CreatePlacementGroup createPlacementGroup) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public CreateSpotDatafeedSubscriptionResponse createSpotDatafeedSubscription(CreateSpotDatafeedSubscription createSpotDatafeedSubscription) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public CreateSubnetResponse createSubnet(CreateSubnet createSubnet) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public CreateTagsResponse createTags(CreateTags createTags) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public CreateVpcResponse createVpc(CreateVpc createVpc) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public CreateVpnConnectionResponse createVpnConnection(CreateVpnConnection createVpnConnection) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public CreateVpnGatewayResponse createVpnGateway(CreateVpnGateway createVpnGateway) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public DeactivateLicenseResponse deactivateLicense(DeactivateLicense deactivateLicense) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public DeleteCustomerGatewayResponse deleteCustomerGateway(DeleteCustomerGateway deleteCustomerGateway) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public DeleteDhcpOptionsResponse deleteDhcpOptions(DeleteDhcpOptions deleteDhcpOptions) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public DeletePlacementGroupResponse deletePlacementGroup(DeletePlacementGroup deletePlacementGroup) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public DeleteSpotDatafeedSubscriptionResponse deleteSpotDatafeedSubscription(DeleteSpotDatafeedSubscription deleteSpotDatafeedSubscription) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public DeleteSubnetResponse deleteSubnet(DeleteSubnet deleteSubnet) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
		
	public DeleteTagsResponse deleteTags(DeleteTags deleteTags) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public DeleteVpcResponse deleteVpc(DeleteVpc deleteVpc) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public DeleteVpnConnectionResponse deleteVpnConnection(DeleteVpnConnection deleteVpnConnection) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public DeleteVpnGatewayResponse deleteVpnGateway(DeleteVpnGateway deleteVpnGateway) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public DescribeBundleTasksResponse describeBundleTasks(DescribeBundleTasks describeBundleTasks) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public DescribeConversionTasksResponse describeConversionTasks(DescribeConversionTasks describeConversionTasks) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public DescribeCustomerGatewaysResponse describeCustomerGateways(DescribeCustomerGateways describeCustomerGateways) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public DescribeDhcpOptionsResponse describeDhcpOptions(DescribeDhcpOptions describeDhcpOptions) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public DescribeLicensesResponse describeLicenses(DescribeLicenses describeLicenses) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public DescribePlacementGroupsResponse describePlacementGroups(DescribePlacementGroups describePlacementGroups) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public DescribeRegionsResponse describeRegions(DescribeRegions describeRegions) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public DescribeReservedInstancesResponse describeReservedInstances(DescribeReservedInstances describeReservedInstances) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public DescribeReservedInstancesOfferingsResponse describeReservedInstancesOfferings(DescribeReservedInstancesOfferings describeReservedInstancesOfferings) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public DescribeSnapshotAttributeResponse describeSnapshotAttribute(DescribeSnapshotAttribute describeSnapshotAttribute) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public DescribeSpotDatafeedSubscriptionResponse describeSpotDatafeedSubscription(DescribeSpotDatafeedSubscription describeSpotDatafeedSubscription) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public DescribeSpotInstanceRequestsResponse describeSpotInstanceRequests(DescribeSpotInstanceRequests describeSpotInstanceRequests) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public DescribeSpotPriceHistoryResponse describeSpotPriceHistory(DescribeSpotPriceHistory describeSpotPriceHistory) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public DescribeSubnetsResponse describeSubnets(DescribeSubnets describeSubnets) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public DescribeTagsResponse describeTags(DescribeTags describeTags) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public DescribeVpcsResponse describeVpcs(DescribeVpcs describeVpcs) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public DescribeVpnConnectionsResponse describeVpnConnections(DescribeVpnConnections describeVpnConnections) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public DescribeVpnGatewaysResponse describeVpnGateways(DescribeVpnGateways describeVpnGateways) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public DetachVpnGatewayResponse detachVpnGateway(DetachVpnGateway detachVpnGateway) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public GetConsoleOutputResponse getConsoleOutput(GetConsoleOutput getConsoleOutput) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public ImportInstanceResponse importInstance(ImportInstance importInstance) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public ImportVolumeResponse importVolume(ImportVolume importVolume) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public ModifyInstanceAttributeResponse modifyInstanceAttribute(ModifyInstanceAttribute modifyInstanceAttribute) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public ModifySnapshotAttributeResponse modifySnapshotAttribute(ModifySnapshotAttribute modifySnapshotAttribute) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
	
	public PurchaseReservedInstancesOfferingResponse purchaseReservedInstancesOffering(PurchaseReservedInstancesOffering purchaseReservedInstancesOffering) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public RequestSpotInstancesResponse requestSpotInstances(RequestSpotInstances requestSpotInstances) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public ResetInstanceAttributeResponse resetInstanceAttribute(ResetInstanceAttribute resetInstanceAttribute) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}

	public ResetSnapshotAttributeResponse resetSnapshotAttribute(ResetSnapshotAttribute resetSnapshotAttribute) {
		throw new EC2ServiceException(ClientError.Unsupported, "This operation is not available");
	}
}
