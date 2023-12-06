import React from "react";
import { SettingLabelStyled, SettingRow } from "./StyledSettingsComponnets";
import { useTranslation } from "react-i18next";
import { onChangeType, FragmentInputParameter, FixedValuesOption, FieldName } from "../../../item";
import { Option, TypeSelect } from "../../../TypeSelect";
import { ExpressionLang } from "../../../../editors/expression/types";
import { EditableEditor } from "../../../../editors/EditableEditor";
import { VariableTypes } from "../../../../../../../types";
import { Error, errorValidator } from "../../../../editors/Validators";
import { EditorType } from "../../../../editors/expression/Editor";

interface InitialValue {
    item: FragmentInputParameter;
    path: string;
    onChange: (path: string, value: onChangeType) => void;
    fieldName: FieldName;
    options?: FixedValuesOption[];
    readOnly: boolean;
    variableTypes: VariableTypes;
    fieldsErrors: Error[];
}

export default function InitialValue({
    onChange,
    fieldName,
    item,
    path,
    options,
    readOnly,
    variableTypes,
    fieldsErrors = [],
}: InitialValue) {
    const { t } = useTranslation();

    const emptyOption = { label: "", value: "" };
    const optionsToDisplay: Option[] = [emptyOption, ...(options ?? []).map(({ label }) => ({ label, value: label }))];

    return (
        <SettingRow>
            <SettingLabelStyled>{t("fragment.initialValue", "Initial value:")}</SettingLabelStyled>
            {options ? (
                <TypeSelect
                    fieldName={fieldName}
                    onChange={(value) => {
                        const selectedOption = options.find((option) => option.label === value);
                        onChange(`${path}.initialValue`, selectedOption);
                    }}
                    value={optionsToDisplay.find((option) => option.value === item?.initialValue?.label)}
                    options={optionsToDisplay}
                    readOnly={readOnly}
                    fieldErrors={fieldsErrors}
                    placeholder={""}
                />
            ) : (
                <EditableEditor
                    fieldName={fieldName}
                    expressionObj={{ language: ExpressionLang.SpEL, expression: item?.initialValue?.label }}
                    onValueChange={(value) => onChange(`${path}.initialValue`, { label: value, expression: value })}
                    variableTypes={variableTypes}
                    readOnly={readOnly}
                    errors={fieldsErrors}
                    param={{ validators: [errorValidator], editor: { type: EditorType.RAW_PARAMETER_EDITOR } }}
                    showValidation
                />
            )}
        </SettingRow>
    );
}