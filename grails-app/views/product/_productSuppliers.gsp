<div class="box">
    <h2>
        <warehouse:message code="product.productSuppliers.label" default="Product Sources"/>
    </h2>

    <div class="dialog">
        <table>
            <thead>
            <g:sortableColumn property="code" title="${warehouse.message(code: 'default.code.label', default: 'Code')}" />

            <th><g:message code="productSupplier.productCode.label" /></th>

            <th><g:message code="default.name.label" default="Name" /></th>

            <th><g:message code="productSupplier.supplier.label" default="Supplier" /></th>

            <th><g:message code="productSupplier.supplierCode.label" default="Supplier Code" /></th>

            <th><g:message code="productSupplier.manufacturer.label" default="Manufacturer" /></th>

            <th><g:message code="productSupplier.manufacturerCode.label" default="Manufacturer Code" /></th>

            <th><g:message code="productSupplier.preferenceTypeCode.label" default="Preference Type" /></th>

            <th><g:message code="productSupplier.ratingTypeCode.label" default="Rating Type" /></th>

            <th><g:message code="productSupplier.unitOfMeasure.label" default="Unit of Measure" /></th>

            <th><g:message code="productSupplier.unitPrice.label" default="Unit Price" /></th>

            <th><g:message code="default.actions.label" default="Actions" /></th>


            </thead>
            <tbody>
                <g:if test="${productInstance?.productSuppliers}">

                    <g:each var="productSupplier" in="${productInstance?.productSuppliers.sort()}" status="status">
                        <tr class="prop ${status%2==0?'odd':'even'}">
                            <td>${fieldValue(bean: productSupplier, field: "code")?:g.message(code:'default.none.label')}</td>

                            <td>${fieldValue(bean: productSupplier, field: "productCode")?:g.message(code:'default.none.label')}</td>

                            <td>${fieldValue(bean: productSupplier, field: "name")?:g.message(code:'default.none.label')}</td>

                            <td>${fieldValue(bean: productSupplier, field: "supplier")}</td>

                            <td>${fieldValue(bean: productSupplier, field: "supplierCode")}</td>

                            <td>${fieldValue(bean: productSupplier, field: "manufacturer")}</td>

                            <td>${fieldValue(bean: productSupplier, field: "manufacturerCode")}</td>

                            <td>${fieldValue(bean: productSupplier, field: "preferenceTypeCode")}</td>

                            <td>${fieldValue(bean: productSupplier, field: "ratingTypeCode")}</td>

                            <td>${fieldValue(bean: productSupplier, field: "unitOfMeasure")}</td>

                            <td>
                                <g:hasRoleFinance onAccessDenied="${g.message(code:'errors.blurred.message', args: ['0.00'])}">
                                    ${fieldValue(bean: productSupplier, field: "unitPrice")}
                                    ${grailsApplication.config.openboxes.locale.defaultCurrencyCode}
                                </g:hasRoleFinance>
                            </td>

                            <td>
                                <a href="javascript:void(0);" class="btn-show-dialog button"
                                   data-position="top"
                                   data-title="${g.message(code:'productSupplier.label')}"
                                   data-url="${request.contextPath}/productSupplier/dialog?id=${productSupplier?.id}&product.id=${productInstance?.id}">
                                    <img src="${resource(dir:'images/icons/silk', file:'pencil.png')}" />
                                    <g:message code="default.button.edit.label"/>
                                </a>
                            </td>
                        </tr>
                    </g:each>
                </g:if>
                <g:unless test="${productInstance?.productSuppliers}">
                    <tr class="prop">
                        <td class="empty center" colspan="11">
                            <g:message code="productSuppliers.empty.label" default="There are no product suppliers"/>
                        </td>
                    </tr>
                </g:unless>
            </tbody>
            <tfoot>
            <tr>
                <td colspan="12">
                    <div class="center">
                        <button class="button icon add btn-show-dialog" data-position="top"
                                data-title="${g.message(code: 'default.add.label', args: [g.message(code:'productSupplier.label')])}"
                                data-url="${request.contextPath}/productSupplier/dialog?product.id=${productInstance?.id}">
                            ${g.message(code: 'default.create.label', default: 'Create', args: [g.message(code:'productSupplier.label')])}
                        </button>
                    </div>
                </td>
            </tr>
            </tfoot>
        </table>
    </div>
</div>


<g:javascript>
    $(document).ready(function() {
        $(".tabs").livequery(function() {
            $(this).tabs({});
        });
    });
</g:javascript>
