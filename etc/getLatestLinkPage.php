<?php

class FileInfo
{

    public $file;
    public $majorVersion;
    public $minorVersion;
    public $subVersion;
    public $lastChange;
    public $isReleaseVersion;
    public $isSetup;
    public $setupType;
    public $fileExtension;
    public $procArch;
}

function getFileInfo($fileName)
{

    if (!isset($fileName)) {
        return null;
    }

    // print_r($fileName);
    // $success = preg_match('/BiDiB-Monitor-(Setup)?-?([0-9]{1,2}).([0-9]{1,2})[.|-]([0-9]{1,5})[-]?([0-9]{8})?/', $fileName, $matches);
    // $success = preg_match('/-([0-9]{1,2}).([0-9]{1,2})-?([0-9]{8}.[0-9]{6}|SNAPSHOT)?-([0-9]{1,4}).(jar|dmg)/', $fileName, $matches);
    $success = preg_match('/-([0-9]{1,2}).([0-9]{1,2})-?([0-9]{8}.[0-9]{6}|SNAPSHOT)?-([0-9]{1,4})(-(x86|x64|a64))?.(jar|dmg|msi)/', $fileName, $matches);
    if (!$success) {
        return null;
    }
    
    // -----
    // print_r($matches);
    
    $fileInfo = new FileInfo();
    $fileInfo->file = $fileName;
    $fileInfo->majorVersion = $matches[1];
    $fileInfo->minorVersion = $matches[2];
    $fileInfo->subVersion = $matches[3];
    $fileInfo->procArch = $matches[6];
    $fileInfo->fileExtension = $matches[7];


    if (file_exists($fileName)) {
        $fileInfo->lastChange = filectime($fileName);
    }

    return $fileInfo;
}

function getAllFiles()
{
    $dir    = '.';
    $files = scandir($dir);
    $fileInfos = array();
    foreach ($files as $value) {
        if ($value == '.' || $value == '..') continue;
        // print_r($value);
        if(!preg_match('/(.jar|.dmg|.msi)/', strtolower($value))) continue;

        $fileInfo = getFileInfo($value);

        // print_r($fileInfo);
        $fileInfos[] = $fileInfo;
    }

    return $fileInfos;
}

function getFilesMatchingVersion($fileInfo, $existingFiles){
    // print_r($existingFiles);

    

    $matchingVersionFiles = array_filter($existingFiles, function ($v) use ($fileInfo) {
        if (!isset($v) || !isset($fileInfo)) {
            return false;
        }

        $matchMajor = isset($fileInfo->majorVersion) 
        ? $v->majorVersion == $fileInfo->majorVersion
        : true;

        $matchMinor = isset($fileInfo->minorVersion) 
        ? $v->minorVersion == $fileInfo->minorVersion
        : true;

        $matchProcArch = isset($fileInfo->procArch) 
        ? $v->procArch == $fileInfo->procArch
        : true;
        
        if( $fileInfo->fileExtension == 'jar') {
            $matchProcArch = true;
        }
        
        return $matchMajor && $matchMinor && $matchProcArch
        && $v->fileExtension == $fileInfo->fileExtension;
    });

    return $matchingVersionFiles;
}

function getLatestVersion($fileInfo, $existingFiles){

    $matchingVersionFiles = getFilesMatchingVersion($fileInfo, $existingFiles);

    // print_r($matchingVersionFiles);
    // echo "got matching files";
    function cmp($a, $b)
    {
        return - (strcmp($a->lastChange, $b->lastChange));
    }

    usort($matchingVersionFiles, "cmp");
    // print_r($matchingVersionFiles);
    // print_r(count($matchingVersionFiles));
    if (count($matchingVersionFiles) > 0) {
        return $matchingVersionFiles[0]->file;
    }

}
 
    $fileType = "jar";
    if (isset($_REQUEST["type"])) {
        $fileType = urldecode($_REQUEST["type"]);
    }

    $procArchType = null;
    if (isset($_REQUEST["procArchType"])) {
        $procArchType = urldecode($_REQUEST["procArchType"]);
    }

    $fileInfo = new FileInfo();
    $fileInfo->majorVersion = 0;
    $fileInfo->minorVersion = 0;
    $fileInfo->subVersion = 0;
    $fileInfo->procArch = $procArchType;
    $fileInfo->fileExtension = $fileType;
	
	
//	$wizardVersion = "2.0";

    if (isset($_REQUEST["version"])) {
//		$wizardVersion = urldecode($_REQUEST["version"]);

        // print_r(urldecode($_REQUEST["version"]));
        $success = preg_match('/([0-9]{1,2}).?([0-9]{1,2})?.?([0-9]{1,2})?/', urldecode($_REQUEST["version"]), $versionMatches);

        if($success){
            // print_r($versionMatches);
            $fileInfo->majorVersion = $versionMatches[1];

            if(count($versionMatches)>1){
                $fileInfo->minorVersion = $versionMatches[2];
            }
        }
    }
    //  print_r($fileInfo);

    // get info for existing files
    $files = getAllFiles();    

    $latestVersionInfo = getLatestVersion($fileInfo, $files);

    if(isset($latestVersionInfo)){
		
//		$url = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') || $_SERVER['SERVER_PORT'] === 443 ? "https://" : "http://";

$url = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$url = rtrim($url, "getLatestLink.php");

$page_url = "{$protocol}{$_SERVER['HTTP_HOST']}{$url}";

//print_r("page_url: {$page_url}<br/>");

//$url_components = parse_url($_SERVER['REQUEST_URI']);
//print_r("url_components: ");
//print_r($url_components);
//print_r("<br/>");
		print_r("<html><body>");
        print_r("Download latest Wizard {$wizardVersion}: <a rel=\"canonical\" href=\"{$page_url}{$latestVersionInfo}\">{$page_url}{$latestVersionInfo}</a>");
		print_r("</body></html>");
    }

