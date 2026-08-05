; =============================================
; Sanda Data Saver & PC Cleaner v1.0.12i
; Inno Setup Script - Gold Master
; By Bishop Dr. David Sanda
; Fixed: Logo everywhere (embedded), 9 apps, Reset, Search, Health, Cleaner
; =============================================

#define MyAppName "Sanda Data Saver"
#define MyAppVersion "1.0.12i"
#define MyAppVersionInfo "1.0.12.0"
#define MyAppPublisher "Bishop Dr. David Sanda"
#define MyAppURL "https://www.davidsanda.com/apps"
#define MyAppExeName "SandaDataSaver.exe"

[Setup]
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL=mailto:sandadatasaver@gmail.com
AppCopyright=Copyright (C) 2026 Bishop Dr. David Sanda - Free for Jesus
DefaultDirName={commonpf}\SandaDataSaver
DefaultGroupName=Sanda Data Saver
OutputDir=InstallerOutput
OutputBaseFilename=SandaDataSaver_Setup_v{#MyAppVersion}
Compression=lzma
SolidCompression=yes
PrivilegesRequired=admin
PrivilegesRequiredOverridesAllowed=dialog
WizardStyle=modern
WizardSizePercent=120
DisableWelcomePage=no
DisableDirPage=no
DisableProgramGroupPage=yes
SetupIconFile=assets\sanda_icon.ico
UninstallDisplayIcon={app}\{#MyAppExeName}
MinVersion=10.0
UninstallDisplayName={#MyAppName} v{#MyAppVersion}
VersionInfoVersion={#MyAppVersionInfo}
VersionInfoCompany={#MyAppPublisher}
VersionInfoDescription=Sanda Data Saver - Smart Data. Your Control.
VersionInfoCopyright=© 2026 Bishop Dr. David Sanda - Free for Jesus
VersionInfoProductName={#MyAppName}
VersionInfoProductVersion={#MyAppVersionInfo}
ArchitecturesInstallIn64BitMode=x64compatible
ChangesAssociations=no
CloseApplications=force
RestartApplications=no
ShowLanguageDialog=no

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: desktopicon; Description: "Create a Desktop Shortcut"; GroupDescription: "Additional options:"; Flags: unchecked
Name: autostart; Description: "Start automatically when Windows starts"; GroupDescription: "Additional options:"; Flags: unchecked
Name: quicklaunchicon; Description: "Create a Quick Launch icon"; GroupDescription: "Additional options:"; Flags: unchecked; OnlyBelowVersion: 0,6.1

[Files]
; Main EXE
Source: "dist\{#MyAppExeName}"; DestDir: "{app}"; Flags: ignoreversion
; Assets - logo and icon for runtime (ensures _install_logo finds them)
Source: "assets\sanda_logo.png"; DestDir: "{app}\assets"; Flags: ignoreversion
Source: "assets\sanda_icon.ico"; DestDir: "{app}\assets"; Flags: ignoreversion
; Also copy to app root for compatibility (old code looks in root)
Source: "assets\sanda_logo.png"; DestDir: "{app}"; DestName: "sanda_logo.png"; Flags: ignoreversion
Source: "assets\sanda_icon.ico"; DestDir: "{app}"; DestName: "sanda_icon.ico"; Flags: ignoreversion
; Version file
Source: "VERSION.txt"; DestDir: "{app}"; Flags: ignoreversion
; Docs (optional)
Source: "README.md"; DestDir: "{app}"; Flags: ignoreversion isreadme; Permissions: everyone-readexec

[Dirs]
Name: "{userappdata}\SandaDataSaver"; Permissions: everyone-modify

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Comment: "Launch Sanda Data Saver v{#MyAppVersion} - Smart Data. Your Control."; IconFilename: "{app}\assets\sanda_icon.ico"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{commondesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon; Comment: "Sanda Data Saver v{#MyAppVersion}"; IconFilename: "{app}\assets\sanda_icon.ico"
Name: "{userappdata}\Microsoft\Internet Explorer\Quick Launch\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: quicklaunchicon
Name: "{commonstartup}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: autostart; Comment: "Sanda Data Saver Auto Start"; IconFilename: "{app}\assets\sanda_icon.ico"

[Registry]
Root: HKCU; Subkey: "SOFTWARE\Microsoft\Windows\CurrentVersion\Run"; ValueType: string; ValueName: "SandaDataSaver"; ValueData: """{app}\{#MyAppExeName}"""; Flags: uninsdeletevalue; Tasks: autostart

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "Launch Sanda Data Saver now"; Flags: nowait postinstall runascurrentuser skipifsilent

[UninstallRun]
Filename: "taskkill"; Parameters: "/F /IM {#MyAppExeName}"; Flags: runhidden; RunOnceId: "KillSanda"

[UninstallDelete]
Type: filesandordirs; Name: "{app}\assets"
Type: filesandordirs; Name: "{userappdata}\SandaDataSaver\__pycache__"

[Code]
var
  IsUpgrade: Boolean;

function IsAppRunning(): Boolean;
var
  WbemLocator, WbemServices, WbemObjectSet: Variant;
begin
  Result := False;
  try
    WbemLocator := CreateOleObject('WbemScripting.SWbemLocator');
    WbemServices := WbemLocator.ConnectServer('.', 'root\CIMV2');
    WbemObjectSet := WbemServices.ExecQuery('SELECT * FROM Win32_Process WHERE Name = "' + '{#MyAppExeName}' + '"');
    Result := (WbemObjectSet.Count > 0);
  except
    Result := False;
  end;
end;

function InitializeSetup(): Boolean;
begin
  Result := True;
  // Check if app is running
  if IsAppRunning() then
  begin
    if MsgBox('Sanda Data Saver is currently running.' + #13#10 + 'Please close it before installing.' + #13#10#13#10 + 'Right-click the tray icon and select Exit, then click OK to continue, or Cancel to abort.', mbError, MB_OKCANCEL) = IDCANCEL then
    begin
      Result := False;
      Exit;
    end;
    // Try to kill it
    Exec('taskkill', '/F /IM {#MyAppExeName}', '', SW_HIDE, ewWaitUntilTerminated, Result);
    Sleep(2000);
  end;
  
  // Check for upgrade
  IsUpgrade := DirExists(ExpandConstant('{app}'));
end;

function InitializeUninstall(): Boolean;
var
  ResultCode: Integer;
begin
  Result := True;
  if IsAppRunning() then
  begin
    if MsgBox('Sanda Data Saver is currently running.' + #13#10 + 'It will be closed before uninstalling.' + #13#10#13#10 + 'Continue?', mbConfirmation, MB_YESNO) = IDYES then
    begin
      Exec('taskkill', '/F /IM {#MyAppExeName}', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Sleep(1500);
      Result := True;
    end
    else
      Result := False;
  end;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  DeleteSettings: Integer;
begin
  if CurUninstallStep = usPostUninstall then
  begin
    DeleteSettings := MsgBox('Do you want to remove your saved settings, logs, and blocked apps list?' + #13#10#13#10 + 'Click Yes to remove everything (clean uninstall).' + #13#10 + 'Click No to keep your settings for future install.' + #13#10 + 'Click Cancel to keep everything.', mbConfirmation, MB_YESNOCANCEL);
    if DeleteSettings = IDYES then
    begin
      DelTree(ExpandConstant('{userappdata}\SandaDataSaver'), True, True, True);
    end;
  end;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
  begin
    // Create AppData folder for logo
    ForceDirectories(ExpandConstant('{userappdata}\SandaDataSaver'));
    // Copy logo to AppData for immediate use
    FileCopy(ExpandConstant('{app}\assets\sanda_logo.png'), ExpandConstant('{userappdata}\SandaDataSaver\sanda_logo.png'), False);
    FileCopy(ExpandConstant('{app}\assets\sanda_logo.png'), ExpandConstant('{userappdata}\SandaDataSaver\logo.png'), False);
  end;
end;

[Messages]
WelcomeLabel1=Welcome to Sanda Data Saver v{#MyAppVersion} Setup
WelcomeLabel2=This software is provided 100% free of charge for the Glory of Jesus Christ, my Savior.%n%nSanda Data Saver is a powerful 2-in-1 tool that helps you:%n%n  🛡️ Shield  Save mobile hotspot data automatically%n  🧹 Broom  Clean your PC and free disk space%n  🚫 Block  Block background apps with one click (9 defaults)%n  🔍 Search  Search installed apps by name (<5 sec)%n  💚 Health  Health reminders with Bible verses%n  🎨 Logo  New Sanda logo everywhere (embedded)%n%nVersion {#MyAppVersion} — GOLD MASTER%nBy Bishop Dr. David Sanda%n%nGiven freely in service to God's people.%n%n“Freely you have received; freely give.” — Matthew 10:8 ✝️
FinishedLabel=Setup has finished installing [name] on your computer.%n%n🛡️ Sanda Data Saver is now ready to protect your hotspot data!%n%nClick Finish to launch the application.
