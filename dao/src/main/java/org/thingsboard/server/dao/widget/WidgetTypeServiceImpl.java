/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.widget;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.Validator;

import java.util.List;
import java.util.Optional;

@Service("WidgetTypeDaoService")
@Slf4j
public class WidgetTypeServiceImpl extends AbstractEntityService implements WidgetTypeService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_RESOURCE_ID = "Incorrect resourceId ";
    public static final String INCORRECT_BUNDLE_ALIAS = "Incorrect bundleAlias ";

    @Autowired
    private WidgetTypeDao widgetTypeDao;

    @Autowired
    private DataValidator<WidgetTypeDetails> widgetTypeValidator;

    @Autowired
    protected ApplicationEventPublisher eventPublisher;

    @Override
    public WidgetType findWidgetTypeById(TenantId tenantId, WidgetTypeId widgetTypeId) {
        log.trace("Executing findWidgetTypeById [{}]", widgetTypeId);
        Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
        return widgetTypeDao.findWidgetTypeById(tenantId, widgetTypeId.getId());
    }

    @Override
    public WidgetTypeDetails findWidgetTypeDetailsById(TenantId tenantId, WidgetTypeId widgetTypeId) {
        log.trace("Executing findWidgetTypeDetailsById [{}]", widgetTypeId);
        Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
        return widgetTypeDao.findById(tenantId, widgetTypeId.getId());
    }

    @Override
    public WidgetTypeDetails saveWidgetType(WidgetTypeDetails widgetTypeDetails) {
        log.trace("Executing saveWidgetType [{}]", widgetTypeDetails);
        widgetTypeValidator.validate(widgetTypeDetails, WidgetType::getTenantId);
        try {
            WidgetTypeDetails result = widgetTypeDao.save(widgetTypeDetails.getTenantId(), widgetTypeDetails);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(result.getTenantId())
                    .entityId(result.getId()).added(widgetTypeDetails.getId() == null).build());
            return result;
        } catch (Exception t) {
            checkConstraintViolation(t,
                    "uq_widget_type_fqn", "Widget type with such fqn already exists!");
            throw t;
        }
    }

    @Override
    public WidgetTypeDetails moveWidgetType(TenantId tenantId, WidgetTypeId widgetTypeId, String targetBundleAlias) {
        log.trace("Executing moveWidgetType, widgetTypeId [{}], targetBundleAlias [{}]", widgetTypeId, targetBundleAlias);
        Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
        WidgetTypeDetails widgetTypeDetails = widgetTypeDao.findById(tenantId, widgetTypeId.getId());
        if (widgetTypeDetails != null && !widgetTypeDetails.getBundleAlias().equals(targetBundleAlias)) {
            widgetTypeDetails.setBundleAlias(targetBundleAlias);
            widgetTypeValidator.validate(widgetTypeDetails, WidgetType::getTenantId);
            widgetTypeDetails = widgetTypeDao.save(widgetTypeDetails.getTenantId(), widgetTypeDetails);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(widgetTypeDetails.getTenantId())
                    .entityId(widgetTypeDetails.getId()).added(widgetTypeDetails.getId() == null).build());
        }
        return widgetTypeDetails;
    }

    @Override
    public void deleteWidgetType(TenantId tenantId, WidgetTypeId widgetTypeId) {
        log.trace("Executing deleteWidgetType [{}]", widgetTypeId);
        Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
        widgetTypeDao.removeById(tenantId, widgetTypeId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(widgetTypeId).build());
    }

    @Override
    public WidgetTypeDetails setWidgetTypeDeprecated(TenantId tenantId, WidgetTypeId widgetTypeId, boolean deprecated) {
        log.trace("Executing setWidgetTypeDeprecated, widgetTypeId [{}], deprecated [{}]", widgetTypeId, deprecated);
        Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
        WidgetTypeDetails widgetTypeDetails = widgetTypeDao.findById(tenantId, widgetTypeId.getId());
        if (widgetTypeDetails.isDeprecated() != deprecated) {
            widgetTypeDetails.setDeprecated(deprecated);
            widgetTypeDetails = widgetTypeDao.save(widgetTypeDetails.getTenantId(), widgetTypeDetails);
        }
        return widgetTypeDetails;
    }

    @Override
    public List<WidgetType> findWidgetTypesByTenantIdAndBundleAlias(TenantId tenantId, String bundleAlias) {
        log.trace("Executing findWidgetTypesByTenantIdAndBundleAlias, tenantId [{}], bundleAlias [{}]", tenantId, bundleAlias);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(bundleAlias, INCORRECT_BUNDLE_ALIAS + bundleAlias);
        return widgetTypeDao.findWidgetTypesByTenantIdAndBundleAlias(tenantId.getId(), bundleAlias);
    }

    @Override
    public List<WidgetTypeDetails> findWidgetTypesDetailsByTenantIdAndBundleAlias(TenantId tenantId, String bundleAlias) {
        log.trace("Executing findWidgetTypesDetailsByTenantIdAndBundleAlias, tenantId [{}], bundleAlias [{}]", tenantId, bundleAlias);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(bundleAlias, INCORRECT_BUNDLE_ALIAS + bundleAlias);
        return widgetTypeDao.findWidgetTypesDetailsByTenantIdAndBundleAlias(tenantId.getId(), bundleAlias);

    }

    @Override
    public List<WidgetTypeInfo> findWidgetTypesInfosByTenantIdAndBundleAlias(TenantId tenantId, String bundleAlias) {
        log.trace("Executing findWidgetTypesInfosByTenantIdAndBundleAlias, tenantId [{}], bundleAlias [{}]", tenantId, bundleAlias);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(bundleAlias, INCORRECT_BUNDLE_ALIAS + bundleAlias);
        return widgetTypeDao.findWidgetTypesInfosByTenantIdAndBundleAlias(tenantId.getId(), bundleAlias);
    }

    @Override
    public List<WidgetTypeDetails> findWidgetTypesInfosByTenantIdAndResourceId(TenantId tenantId, TbResourceId tbResourceId) {
        log.trace("Executing findWidgetTypesInfosByTenantIdAndResourceId, tenantId [{}], tbResourceId [{}]", tenantId, tbResourceId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(tbResourceId, INCORRECT_RESOURCE_ID + tbResourceId);
        return widgetTypeDao.findWidgetTypesInfosByTenantIdAndResourceId(tenantId.getId(), tbResourceId.getId());
    }

    @Override
    public WidgetType findWidgetTypeByTenantIdAndFqn(TenantId tenantId, String fqn) {
        log.trace("Executing findWidgetTypeByTenantIdAndFqn, tenantId [{}], fqn [{}]", tenantId, fqn);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(fqn, "Incorrect fqn " + fqn);
        return widgetTypeDao.findByTenantIdAndFqn(tenantId.getId(), fqn);
    }

    @Override
    public void deleteWidgetTypesByTenantIdAndBundleAlias(TenantId tenantId, String bundleAlias) {
        log.trace("Executing deleteWidgetTypesByTenantIdAndBundleAlias, tenantId [{}], bundleAlias [{}]", tenantId, bundleAlias);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(bundleAlias, INCORRECT_BUNDLE_ALIAS + bundleAlias);
        List<WidgetType> widgetTypes = widgetTypeDao.findWidgetTypesByTenantIdAndBundleAlias(tenantId.getId(), bundleAlias);
        for (WidgetType widgetType : widgetTypes) {
            deleteWidgetType(tenantId, new WidgetTypeId(widgetType.getUuidId()));
        }
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findWidgetTypeById(tenantId, new WidgetTypeId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.WIDGET_TYPE;
    }

}
