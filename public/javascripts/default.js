document.addEventListener('DOMContentLoaded', function () {
    $(function () {
        $('[data-toggle="tooltip"]').tooltip()
    });

    $("#createTeamForm").submit(function (event) {
        event.preventDefault();
        try {
            pks = JSON.parse($("#members").val())
        } catch (e) {
            alert("Enter a valid list of members " + e.message);
            return
        }
        if (pks.length === 0) {
            alert("member list can not be empty!");
            return
        } else {
            for (i = 0; i < pks.length; i++) {
                if (!pks[i].hasOwnProperty('nick') || !pks[i].hasOwnProperty('pk')) {
                    alert("enter a valid member list like the example.");
                    return

                }
            }
        }
        data = {
            name: $("#name").val(),
            address: $("#address").val(),
            description: $("#description").val(),
            members: pks
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

