import React from "react";
import { onChangeType, FragmentInputParameter, isStringOrBooleanVariant } from "../item";
import { FixedValuesPresets, VariableTypes } from "../../../../../types";
import { DefaultVariant, StringBooleanVariant } from "./variants";
import { Error } from "../../editors/Validators";

interface Settings {
    item: FragmentInputParameter;
    path: string;
    variableTypes: VariableTypes;
    onChange: (path: string, value: onChangeType) => void;
    fixedValuesPresets: FixedValuesPresets;
    readOnly: boolean;
    fieldsErrors: Error[];
}

export function Settings(props: Settings) {
    if (isStringOrBooleanVariant(props.item)) {
        return <StringBooleanVariant {...props} item={props.item} />;
    }

    return <DefaultVariant {...props} item={props.item} />;
}