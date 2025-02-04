$(document).ready(function () {
    document.title = Android.getString('Sync');

    $('#spPleaseWait').text(Android.getString('PleaseWait'));
    $('#UploadEnrollment').text(Android.getString('UploadEnrollment'));
    $('#EnrollmentXML').text(Android.getString('EnrollmentXML'));
    $('#UploadRenewals').text(Android.getString('UploadRenewals'));
    $('#CreateRenewalXML').text(Android.getString('CreateRenewalXML'));
    $('#UploadFeedBack').text(Android.getString('UploadFeedBack'));
    $('#CreateFeedbackXML').text(Android.getString('CreateFeedbackXML'));
    $('#UploadPhoto').text(Android.getString('UploadPhoto'));
    $('#DownloadMaster').text(Android.getString('DownloadMaster'));
    $('#ControlNumbers').text(Android.getString('ControlNumbers'));



    $(".ulList li").click(function () {
        try {
            var action = $(this).attr("id");
            switch (action) {
                case "liUploadEnrolment":
                    if (!Android.isLoggedIn()) {
                        window.open("Login.html?s=0", "_self");
                    } else {
                        Android.uploadEnrolment();
                    }
                    break;
                case "liEnrolmentXML":
                    Android.CreateEnrolmentXML();

                    break;
                case "liUploadRenewals":
                    if (!Android.isLoggedIn()) {
                        window.open("Login.html?s=0", "_self");
                    } else {
                        Android.UploadOfflineFeedbackRenewal('renewal');
                    }

                    break;
                case "liCreateRenewalXML":
                    Android.zipFeedBackRenewal('Renewal');
                    break;
                case "liUploadFeedback":
                    if (!Android.isLoggedIn()) {
                        window.open("Login.html?s=0", "_self");
                    } else {
                        Android.UploadOfflineFeedbackRenewal('feedback');
                    }
                    break;
                case "liCreateFeedbackXML":
                    Android.zipFeedBackRenewal('Feedback');
                    break;

                case "liDownloadMasterData":
                    var res = Android.checkNet();
                    if (res == "false") {
                        Android.getLocalData();
                    } else {
                        Android.downloadMasterData();
                    }

                    break;
                case "liUploadPhoto":
                    if (!Android.isLoggedIn()) {
                        window.open("Login.html?s=0", "_self");
                    }
                    Android.UploadPhotos();
                    break;

                case "liControlNumbers":
                    Android.launchControlNumbers();
                    break;
            }
        } catch (e) {
            Android.ShowDialog(e.message);
        }
    });
});
