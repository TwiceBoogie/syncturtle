package com.syncturtle.services.file.service.asset;

import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.file.FileAssetPurpose;
import com.syncturtle.services.file.service.asset.param.FileAssetObjectKeyCreateParam;

@Component
public class FileAssetObjectKeyFactory {

    public String createObjectKey(FileAssetObjectKeyCreateParam param) {
        Assert.notNull(param, "file asset object key create param is required");

        return prefix(param)
                + "/"
                + param.getAssetId()
                + "/"
                + param.getOriginalFilename();
    }

    private static String prefix(FileAssetObjectKeyCreateParam param) {
        FileAssetPurpose purpose = param.getPurpose();
        String purposeSegment = purpose.name().toLowerCase(Locale.ROOT);

        if (purpose == FileAssetPurpose.WORKSPACE_LOGO) {
            Assert.notNull(param.getWorkspaceId(), "workspaceId is required for workspace logo assets");

            return "workspaces/" + param.getWorkspaceId() + "/" + purposeSegment;
        }

        if (purpose == FileAssetPurpose.USER_AVATAR || purpose == FileAssetPurpose.USER_COVER) {
            return "users/" + param.getOwnerUserId() + "/" + purposeSegment;
        }

        return "unscoped/" + purposeSegment;
    }

}
