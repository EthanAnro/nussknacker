import {css} from "@emotion/css"
import React from "react"
import {useSelector} from "react-redux"
import {MenuBar} from "../components/MenuBar"
import {VersionInfo} from "../components/versionInfo"
import {getLoggedUser} from "../reducers/selectors/settings"
import {isEmpty} from "lodash"
import {Outlet} from "react-router-dom"
import {GlobalCSSVariables, NkThemeProvider} from "./theme"
import {Notifications} from "./Notifications"
import {UsageReportingImage} from "./UsageReportingImage"
import {WindowManager} from "../windowManager"

export function NussknackerApp() {
  const loggedUser = useSelector(getLoggedUser)

  if (isEmpty(loggedUser)) {
    return null
  }

  return (
    <NkThemeProvider>
      <GlobalCSSVariables/>
      <WindowManager
        className={css({
          flex: 1,
          display: "flex",
        })}
      >
        <div
          id="app-container"
          className={css({
            flex: 1,
            display: "grid",
            gridTemplateRows: "auto 1fr",
            alignItems: "stretch",
          })}
        >
          <MenuBar/>
          <main className={css({overflow: "auto"})}>
            <Outlet/>
          </main>
        </div>
      </WindowManager>
      <Notifications/>
      <VersionInfo/>
      <UsageReportingImage/>
    </NkThemeProvider>
  )
}
