import { alpha } from "@mui/material";
import { PaletteOptions } from "@mui/material/styles/createPalette";
import { EnvironmentTagColor } from "../EnvironmentTag";

const standardPalette: PaletteOptions = {
    primary: {
        main: "#8256B5",
    },
    secondary: {
        light: "#bf570c",
        main: "#BF360C",
    },
    error: {
        light: "#DE7E8A",
        main: "#B71C1C",
    },
    warning: {
        main: "#FF6F00",
    },
    success: {
        main: "#388E3C",
        dark: `#206920`,
        contrastText: `#FFFFFF`,
    },
    background: {
        paper: "#c6c7d1",
        default: "#9394A5",
    },
    text: {
        primary: "#212121",
        secondary: "#030303",
    },
    action: {
        hover: alpha("#8256B5", 0.08),
        active: alpha("#8256B5", 0.12),
    },
};

// It's for a testing purpose only, to check if all color relations are added. We don't support light mode yet
export const lightModePalette = {
    ...standardPalette,
    custom: {
        environmentAlert: {
            [EnvironmentTagColor.green]: "#80D880",
            [EnvironmentTagColor.yellow]: "#FDE3A0",
            [EnvironmentTagColor.red]: "#DF818C",
            [EnvironmentTagColor.blue]: "#43A1E6",
        },
        nodes: {
            Source: {
                fill: "#509D6E",
            },
            FragmentInputDefinition: {
                fill: "#509D6E",
            },
            Sink: {
                fill: "#DB4646",
            },
            FragmentOutputDefinition: {
                fill: "#DB4646",
            },
            Filter: {
                fill: "#FAA05A",
            },
            Switch: {
                fill: "#1B78BC",
            },
            VariableBuilder: {
                fill: "#FEB58A",
            },
            Variable: {
                fill: "#FEB58A",
            },
            Enricher: {
                fill: "#A171E6",
            },
            FragmentInput: {
                fill: "#A171E6",
            },
            Split: {
                fill: "#F9C542",
            },
            Processor: {
                fill: "#4583dd",
            },
            Aggregate: {
                fill: "#e892bd",
            },
            Properties: {
                fill: "#46ca94",
            },
            CustomNode: {
                fill: "#1EC6BE",
            },
            Join: {
                fill: "#1EC6BE",
            },
            _group: {
                fill: "#1EC6BE",
            },
        },
        windows: {
            compareVersions: { backgroundColor: "#1ba1af", color: "white" },
            customAction: { backgroundColor: "white", color: "black" },
            default: { backgroundColor: "#2D8E54", color: "white" },
        },
    },
};