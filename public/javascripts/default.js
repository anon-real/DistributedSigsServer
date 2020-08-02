document.addEventListener('DOMContentLoaded', function () {
    $(function () {
        $('[data-toggle="tooltip"]').tooltip()
    });

    $("#createTeamForm").submit(function (event) {
        event.preventDefault();
        try {
            pks = JSON.parse($("#pks").val())
        } catch (e) {
            alert("Enter a valid list of publik keys " + e.message);
            return
        }
        if (pks.length === 0) {
            alert("public key list can not be empty!");
            return
        }
        data = {
            name: $("#name").val(),
            address: $("#address").val(),
            description: $("#description").val(),
            pks: pks
        };
        posting = $.ajax({
            url: $(this).attr('action'),
            type: "POST",
            data: JSON.stringify(data),
            contentType: "application/json; charset=utf-8",
            dataType: "json",
            success: function (data, textStatus) {
                if (data.redirect) {
                    window.location.href = data.redirect;
                }
            },
            error: function (data) {
                alert("request failed: " + data.responseJSON.message)
            }
        });
    });

}, false);

