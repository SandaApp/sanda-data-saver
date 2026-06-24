; =============================================
; Sanda Data Saver & PC Cleaner
; Inno Setup Script
; By Bishop David Sanda
; =============================================

[Setup]
AppName=Sanda Data Saver
AppVersion=1.0.1
AppPublisher=Bishop David Sanda
AppPublisherURL=https://www.davidsanda.com/apps
AppSupportURL=mailto:sandadatasaver@gmail.com
AppCopyright=Copyright (C) 2026 Bishop David Sanda
DefaultDirName={commonpf}\SandaDataSaver
DefaultGroupName=Sanda Data Saver
OutputDir=InstallerOutput
OutputBaseFilename=SandaDataSaver_Setup_v1.0.1
Compression=lzma
SolidCompression=yes
PrivilegesRequired=admin
WizardStyle=modern
DisableWelcomePage=no
DisableDirPage=no
DisableProgramGroupPage=yes
SetupIconFile=assets\icon.ico
MinVersion=10.0
UninstallDisplayName=Sanda Data Saver
UninstallDisplayIcon={app}\SandaDataSaver.exe

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: desktopicon; Description: "Create a Desktop Shortcut"; GroupDescription: "Additional options:"
Name: autostart; Description: "Start automatically when Windows starts"; GroupDescription: "Additional options:"

[Files]
Source: "dist\SandaDataSaver.exe"; DestDir: "{app}"; Flags: ignoreversion

[Dirs]
Name: "{userappdata}\SandaDataSaver"

[Icons]
Name: "{group}\Sanda Data Saver"; Filename: "{app}\SandaDataSaver.exe"; Comment: "Launch Sanda Data Saver"
Name: "{commondesktop}\Sanda Data Saver"; Filename: "{app}\SandaDataSaver.exe"; Tasks: desktopicon; Comment: "Launch Sanda Data Saver"
Name: "{commonstartup}\Sanda Data Saver"; Filename: "{app}\SandaDataSaver.exe"; Tasks: autostart; Comment: "Sanda Data Saver Auto Start"
Name: "{group}\Uninstall Sanda Data Saver"; Filename: "{uninstallexe}"

[Registry]
Root: HKCU; Subkey: "SOFTWARE\Microsoft\Windows\CurrentVersion\Run"; ValueType: string; ValueName: "SandaDataSaver"; ValueData: """{app}\SandaDataSaver.exe"""; Flags: uninsdeletevalue; Tasks: autostart

[Run]
Filename: "{app}\SandaDataSaver.exe"; Description: "Launch Sanda Data Saver now"; Flags: nowait postinstall runascurrentuser

[UninstallRun]
Filename: "taskkill"; Parameters: "/F /IM SandaDataSaver.exe"; Flags: runhidden; RunOnceId: "KillSanda"

[UninstallDelete]
Type: filesandordirs; Name: "{userappdata}\SandaDataSaver"

[Code]
function IsAppRunning(): Boolean;
var
  WbemLocator: Variant;
  WbemServices: Variant;
  WbemObjectSet: Variant;
begin
  Result := False;
  try
    WbemLocator := CreateOleObject('WbemScripting.SWbemLocator');
    WbemServices := WbemLocator.ConnectServer('.', 'root\CIMV2');
    WbemObjectSet := WbemServices.ExecQuery('SELECT * FROM Win32_Process WHERE Name = "SandaDataSaver.exe"');
    Result := (WbemObjectSet.Count > 0);
  except
    Result := False;
  end;
end;

function InitializeSetup(): Boolean;
begin
  Result := True;
  if IsAppRunning() then
  begin
    MsgBox('Sanda Data Saver is currently running.' + #13#10 + 'Please close it before installing.' + #13#10#13#10 + 'Right-click the tray icon and select Exit, then run setup again.', mbError, MB_OK);
    Result := False;
  end;
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
      Exec('taskkill', '/F /IM SandaDataSaver.exe', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
      Sleep(1500);
      Result := True;
    end
    else
      Result := False;
  end;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usPostUninstall then
  begin
    if MsgBox('Do you want to remove your saved settings and logs?' + #13#10#13#10 + 'Click Yes to remove everything.' + #13#10 + 'Click No to keep your settings.', mbConfirmation, MB_YESNO) = IDYES then
    begin
      DelTree(ExpandConstant('{userappdata}\SandaDataSaver'), True, True, True);
    end;
  end;
end;

[Messages]
WelcomeLabel1=Welcome to Sanda Data Saver Setup
WelcomeLabel2=This software is provided 100% free of charge for the Glory of Jesus Christ, my Savior.%n%nSanda Data Saver is a powerful 2-in-1 tool that helps you:%n%n  🛡️ Shield  Save mobile hotspot data automatically%n  🧹 Broom  Clean your PC and free disk space%n  🚫 Block  Block background apps with one click%n  📊 Chart  Monitor network activity%n%nVersion 1.0.1%nBy Bishop David Sanda%n%nGiven freely in service to God's people.
