import { css } from "@emotion/css";
import { DefaultContent, DefaultContentProps } from "@touk/window-manager";
import { FooterButtonProps } from "@touk/window-manager/cjs/components/window/footer";
import React, { PropsWithChildren, ReactElement, useMemo, useState } from "react";
import { useKey } from "rooks";
import ErrorBoundary from "../components/common/ErrorBoundary";
import { StyledContent, StyledHeader } from "../components/graph/node-modal/node/StyledHeader";
import { IconModalHeader } from "../components/graph/node-modal/nodeDetails/NodeDetailsModalHeader";
import { isInputTarget } from "../containers/BindKeyboardShortcuts";
import { LoadingButton } from "./LoadingButton";

function cleanList<T extends NonNullable<unknown>>(elements: (T | null | undefined | false | void)[]): T[] {
    return elements.filter(Boolean) as T[];
}

type WindowContentProps = Omit<DefaultContentProps, "buttons"> &
    PropsWithChildren<{
        icon?: ReactElement;
        subheader?: ReactElement;
        buttons?: (FooterButtonProps | false)[];
        closeWithEsc?: boolean;
    }>;

export function WindowContent({ children, icon, subheader, buttons, closeWithEsc, ...props }: WindowContentProps): JSX.Element {
    const classnames = useMemo(
        () => ({
            footer: css({
                justifyContent: "flex-end",
            }),
            ...props.classnames,
        }),
        [props.classnames],
    );

    useKey(
        "Escape",
        (e) => {
            e.preventDefault();
            if (!isInputTarget(e.composedPath().shift())) {
                props.close();
            }
        },
        { when: closeWithEsc },
    );

    const [stableIcon] = useState(icon);
    const [stableSubheader] = useState(subheader);

    const components = useMemo<DefaultContentProps["components"]>(
        () => ({
            Header: StyledHeader,
            Content: StyledContent,
            HeaderTitle: (headerProps) => <IconModalHeader {...headerProps} startIcon={stableIcon} subheader={stableSubheader} />,
            FooterButton: LoadingButton,
            ...props.components,
        }),
        [stableIcon, stableSubheader, props.components],
    );

    return (
        <DefaultContent {...props} buttons={cleanList(buttons)} components={components} classnames={classnames}>
            <ErrorBoundary>{children}</ErrorBoundary>
        </DefaultContent>
    );
}
