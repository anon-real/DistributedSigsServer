document.addEventListener('DOMContentLoaded', function () {

    $(function () {
        let numMembers = 1;
        $(document).on(
            'click',
            '[data-role="dynamic-fields"] > .form-inline [data-role="remove"]',
            function (e) {
                e.preventDefault();
                $(this).closest('.form-inline').remove();
            }
        );
        $(document).on(
            'click',
            '[data-role="dynamic-fields"] > .form-inline [data-role="add"]',
            function (e) {
                e.preventDefault();
                numMembers++;
                var container = $(this).closest('[data-role="dynamic-fields"]');
                new_field_group = container.children().filter('.form-inline:first-child').clone();
                new_field_group.find('input')[0].id = 'member-nick-' + numMembers;
                new_field_group.find('input')[1].id = 'member-pk-' + numMembers;
                new_field_group.find('input').each(function () {
                    $(this).val('');
                });
                container.append(new_field_group);
            }
        );
    });


    $(function () {
        $('[data-toggle="tooltip"]').tooltip()
    });

    $("#assetType").change(function (event) {
        if ($("#assetType").val() == "ERG") {
            $("#tokenId").prop("disabled", true);
            $("#tokenId").val("");
        } else {
            $("#tokenId").prop("disabled", false);
        }
    });

    $("#createTeamForm").submit(function (event) {
        event.preventDefault();
        var pks = [];
        let nicks = $('input[id^="member-nick"]');
        let mpks = $('input[id^="member-pk"]');
        for (let i = 0; i < nicks.length; i++) {
            let dict = {};
            dict["nick"] = nicks[i].value;
            dict["pk"] = mpks[i].value;
            pks.push(dict);
        }
        data = {
            name: $("#name").val(),
            address: $("#address").val(),
            description: $("#description").val(),
            members: pks,
            assetName: $("#assetType").val(),
            tokenId: $("#tokenId").val()
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

