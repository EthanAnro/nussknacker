describe("Connection error", () => {
    const NAME = "no-backend";

    before(() => {
        cy.deleteAllTestProcesses({ filter: NAME, force: true });
    });

    beforeEach(() => {
        cy.viewport(1400, 1000);
        cy.visit("/");
    });

    afterEach(() => {
        cy.goOnline();
    });

    it("should display connection errors", () => {
        const verifyNoNetworkAccess = () => {
            cy.log("verify no network access");
            cy.intercept("/api/notifications").as("notifications");

            cy.wait("@notifications");
            cy.goOffline();
            cy.contains(/No network access/).should("be.visible");
            cy.get("body").matchImage({
                screenshotConfig: {
                    blackout: ["[data-testid=graphPage] > :not(#nk-graph-main) > div"],
                },
            });
            cy.goOnline();
            cy.contains(/No network access/).should("not.exist");
        };

        const verifyNoBackendAccess = () => {
            cy.log("verify no backend access");

            cy.intercept("/api/notifications", { statusCode: 502 });

            cy.contains(/Backend connection issue/).should("be.visible");
            cy.get("body").matchImage({
                screenshotConfig: {
                    blackout: ["[data-testid=graphPage] > :not(#nk-graph-main) > div"],
                },
            });

            cy.intercept("/api/notifications", (req) => {
                req.continue();
            });

            cy.contains(/Backend connection issue/).should("not.exist");
        };

        const verifyNoBackendAccessWhenScenarioEditNodeModalOpens = () => {
            cy.log("verify no backend access when scenario edit modal opens");
            cy.visitNewProcess(NAME, "filter");

            cy.contains("svg", /filter/i).dblclick();
            cy.intercept("/api/notifications", { statusCode: 502 });

            cy.contains(/Backend connection issue/).should("be.visible");
            cy.get("body").matchImage({
                screenshotConfig: {
                    blackout: ["[data-testid=graphPage] > :not(#nk-graph-main) > div"],
                },
            });

            cy.intercept("/api/notifications", (req) => {
                req.continue();
            });

            cy.contains(/Backend connection issue/).should("not.exist");
        };

        verifyNoNetworkAccess();
        verifyNoBackendAccess();
        verifyNoBackendAccessWhenScenarioEditNodeModalOpens();
    });
});