/*global jQuery */
/*jshint globalstrict: true*/
'use strict';
var ufal = ufal || {};
var console = window.console || {
        log: function () {
        }
    };

ufal.submissions = {

    init_openaire: function () {

        // hide the input box
        jQuery(".openaire-id").parent().hide();

        // show if EU is selected
        jQuery(".openaire-type-map").change(function() {
            var type = jQuery("select option:selected").val();
            if ("euFunds" === type) {
                jQuery(".openaire-id").parent().show();
                // indicate we want to fetch from additional source
                jQuery(".openaire-code-autocomplete").attr("autocomplete-openaire", "choices/dc_relation?query=");
            }else {
                jQuery(".openaire-id").parent().hide();
                // remove additional autocomplete source
                jQuery(".openaire-code-autocomplete").removeAttr("autocomplete-openaire");
            }
        });

        // hook to value change
    },

    fix_l10n: function (){
        //because of continuation do a form submit
        // - it shouldn't take us back and the data should remain filled
        var jForm = jQuery("form[action*='continue']");
        if(jForm && jForm.length > 0){
            var action = jForm.attr("action");
            jQuery("a[href*='locale-attribute']").each(function(){
                var jAnchor = jQuery(this);
                jAnchor.click(function(event){
                    event.preventDefault();
                    jForm.attr("action", action + jAnchor.attr("href"));
                    jForm.submit();
                });
            });
        }
    },
    handle_submission_js: function () {
        jQuery
            .each(
                jQuery("#aspect_submission_StepTransformer_list_submit-describe .thumbnail"),
                function () {
                    jQuery(this)
                        .on(
                            "click",
                            function () {
                                jQuery(
                                    "#aspect_submission_StepTransformer_list_submit-describe .thumbnail")
                                    .attr("class",
                                        "thumbnail col-sm-3");
                                jQuery(this)
                                    .attr("class",
                                        "thumbnail col-sm-3 alert-info");
                            });
                });

        var type = jQuery("#aspect_submission_StepTransformer_field_dc_type")
            .val();
        if (type != null || type !== "") {
            jQuery("#type_" + type).attr("class",
                "thumbnail col-sm-3 alert-info");
        }

        jQuery("#aspect_submission_StepTransformer_field_dc_date_issued")
            .datepicker({
                autoclose: true,
                format: 'yyyy-mm-dd',
                forceParse: false
            });

    },

    autocomplete_solr: function (obj, url, updater_function) {
        ufal.submissions.configurable_autocomplete_solr(
            obj,
            url,
            updater_function,
            undefined,
            function (item, values) {
                var used_by = "";
                if (0 < values[item]) {
                    used_by = "<span class='font_smaller'>(" + $.i18n._("autocomplete-used-by", values[item]) + ")</span>";
                }
                return "<dl class='dl-horizontal'>" + "<dd style='margin-left: 10px;'>" + item +
                    " " + used_by + "</dd>" + "</dl>";
            }
        );
    },

    cached_ajax_json: [],

    configurable_autocomplete_solr: function (
        obj, url, updater_function, matcher_function, highlighter_function, source_function
    ) {
        var values = {};
        if (!(url in ufal.submissions.cached_ajax_json)) {
            ufal.submissions.cached_ajax_json[url] = ufal.utils.ajax_json(url);
        }
        var data = ufal.submissions.cached_ajax_json[url];
        if (data == null || !data.hasOwnProperty("facet_counts")) {
            // Try checking the rewrite rule (described in local conf)
            console.log(
                "lindat/autocomplete failed to load data: did not get any solr json");
            return;
        }
        for (var d in data.facet_counts.facet_fields) {
            // get the first property ...
            if (data.facet_counts.facet_fields.hasOwnProperty(d)) {
                data = data.facet_counts.facet_fields[d];
                break;
            }
        }
        if (data instanceof Array) {
            for (var i = 0; i < data.length; i += 2) {
                values[data[i]] = data[i + 1];
            }
        } else if (data instanceof Object) {
            values = data;
        } else {
            console.log(
                "lindat/autocomplete failed to load data: did not get correct solr json");
        }

        obj.typeahead({
            source: function (query, process) {
                if (undefined == source_function) {
                    process(Object.keys(values));
                }else {
                    source_function(values, query, process);
                }
            },
            sorter: function (items) {
                // sort according usage count
                return items.sort(function (i1, i2) {
                    var cnt2 = (i2 in values) ? values[i2] : 0;
                    var cnt1 = (i1 in values) ? values[i1] : 0;
                    return cnt2 - cnt1;
                });
            },
            highlighter: function (item) {
                return highlighter_function(item, values);
            },
            matcher: matcher_function,
            updater: updater_function,
            items: 6
        });
    },

    handle_field_updater: function (item) {
        return 'http://hdl.handle.net/' + item.split(/:/,1)[0];
    },

    autocomplete_solr_handle_field: function (obj, url) {
        ufal.submissions.autocomplete_solr(obj, url,
            ufal.submissions.handle_field_updater);
    },

    name_splitter: function (text) {
        var splits = [];
        if (text != null) {
            splits = text.split(/\s*,\s*/);
        }
        return splits;
    },

    sponsor_splitter: function (text) {
        var splits = [];
        if (text !== null) {
            splits = text.split(/@@/);
        }
        return splits;
    },

    name_field_updater: function (item) {
        return ufal.submissions.field_updater.call(this, ["_last", "_first"], ufal.submissions.name_splitter, item);
    },

    field_updater: function (selector_parts, splitter, item) {
        var id = this.$element.attr('id');
        var pattern = selector_parts.join('$|') + '$';
        var base_id = id.replace(new RegExp(pattern), "");
        var selectors = jQuery.map(selector_parts, function (element, index) {
            return "#" + base_id + element;
        });
        var splits = splitter(item);
        var return_value = null;
        if (splits.length > 0) {
            for (var i = 0; i < selectors.length; i++) {
                var selector = selectors[i];
                var value = "";
                if (i < splits.length) {
                    value = splits[i];
                }
                var element = jQuery(selector);
                if (element !== null) {
                    if (element.attr('id') === id) {
                        return_value = value;
                    }
                    element.val(value);
                }
            }
        }
        return return_value;
    },

    autocomplete_solr_name_field: function (obj, url) {
        ufal.submissions.autocomplete_solr(obj, url,
            ufal.submissions.name_field_updater);
    },

    autocomplete_solr_sponsor_component: function (obj, url) {
        ufal.submissions.configurable_autocomplete_solr(
            obj, url,
            // updater
            function (item) {
                return ufal.submissions.field_updater.call(
                    this,
                    ["_1_orgname", "_2_code", "_3_projname", "_4_type", "_5_openaire_id"],
                    ufal.submissions.sponsor_splitter,
                    item
                );
            },
            // matcher
            function (item) {
                var id = this.$element.attr('id');
                var splits = ufal.submissions.sponsor_splitter(item);
                var value = splits[1] + "\t" + splits[2];
                //default matcher but changed value
                return ~value.toLowerCase().indexOf(this.query.toLowerCase());
            },
            // highligher
            function (item, values) {
                var splits = ufal.submissions.sponsor_splitter(item);
                var funding_code = splits[1];
                var project_name = splits[2];
                var used_by = "";
                if (item in values && 0 < values[item]) {
                    used_by = "<br /><span class='font_smaller label label-primary'>" + $.i18n._("autocomplete-used-by-submission", values[item]) + "</span>";
                }
                return "<dl class='dl-horizontal'>" +
                    "<dt style='width: 110px;'>" + $.i18n._("autocomplete-funding-code") + "</dt>" +
                    "<dd style='padding-left: 0px;'>" +
                    funding_code +
                    "</dd>" +
                    "<dt style='width: 110px;'>" + $.i18n._("autocomplete-project-name") + "</dt>" +
                    "<dd style='padding-left: 0px;'>" +
                    project_name +
                    used_by +
                    "</dd></dl>";
            },
            // source
            function (values, query, process) {
                // query backend for OpenAIRE if euFUNDS
                var attr = obj.attr("autocomplete-openaire");
                if (typeof attr !== typeof undefined && attr !== false) {
                    var choices_spec = obj.attr("autocomplete-openaire");
                    jQuery.ajax({
                        type: 'GET',
                        url: ufal.utils.get_dspace_url() + choices_spec + query,
                        dataType: 'xml',
                        success: function (data) {
                            var xml = jQuery(data);
                            var separator = "@@";
                            xml.find("Choice").each(function(){
                                // fake number of usages to -1
                                var project_name = jQuery(this).text();
                                project_name = project_name.substr(
                                    project_name.indexOf("-") + 2
                                );
                                var openaire_id = jQuery(this).attr("value");
                                // e.g., EC/FP7/..
                                var project_id = openaire_id.substr(openaire_id.split("/", 2).join("/").length + 1);
                                var fund_type = jQuery(".openaire-type-map").val();
                                var format = "European Union"
                                    + separator + project_id
                                    + separator + project_name
                                    + separator + fund_type
                                    + separator + openaire_id;
                                values[format] = -1;
                            });
                        },
                    }).fail(function (jqXHR, textStatus) {
                        console.log("lindat/utils failed to load openaire data...");
                    }).done(function () {
                        process(Object.keys(values));
                    });

                }else {
                    process(Object.keys(values));
                }
            }
        );
    },


    single_field_updater: function (item) {
        return item;
    },

    autocomplete_solr_single_field: function (obj, url) {
        ufal.submissions.autocomplete_solr(obj, url,
            ufal.submissions.single_field_updater);
    },

    autocomplete_lang_codes: function (jobj, url) {
        var lang_pairs_iso = ufal.utils.ajax_json(url);
        jQuery("#aspect_submission_StepTransformer_field_dc_language_iso")
            .typeahead(
                {
                    source: Object.keys(lang_pairs_iso),
                    highlighter: function (item) {
                        return "<dl class='dl-horizontal'>"
                            + "<dt style='width: 80px;'>" + $.i18n._("autocomplete-iso") + "</dt>"
                            + "<dd style='margin-left: 100px;'>"
                            + lang_pairs_iso[item]
                            + "</dd>"
                            + "<dt style='width: 80px;'>" + $.i18n._("autocomplete-language") + "</dt>"
                            + "<dd style='margin-left: 100px;'>"
                            + item + "</dd>" + "</dl>";
                    },
                    updater: function (item, scope) {
                        return lang_pairs_iso[item];
                    },
                    items: 8
                }).blur(function () {
            var lang = jobj.val();
            for (var p in lang_pairs_iso) {
                if (lang_pairs_iso[p] === lang) {
                    return;
                }
            }
            var warning_cls = "alert alert-warning";
            // iso 693-3 must have 3 letters (even N/A)!
            if (3 != lang.length) {
                jobj.parent().find(".text-info").addClass(warning_cls);
                jobj.addClass(warning_cls);
            } else {
                jobj.parent().find(".text-info").removeClass(warning_cls);
                jobj.removeClass(warning_cls);
            }
        });
    },

    autocomplete_select2: function (jobj) {
        jobj.select2({
            placeholder: 'Please select value',
            allowClear: true
        });
    },

    autocomplete: function () {
        jQuery(".autocomplete")
            .each(
                function () {
                    // two hidden inputs specify the url and the type
                    var type = jQuery(
                        "[name='" + jQuery(this).attr('name') + "-type']").val();
                    var type_splits = type.split("-");
                    var basetype = type_splits[0];

                    if (basetype === "select2") {
                        ufal.submissions
                            .autocomplete_select2(jQuery(this));
                    } else if (basetype === "solr"
                        || basetype === "json_static") {

                        var subtype = "";
                        if (type_splits.length >= 2) {
                            subtype = type_splits[1];
                        }

                        var url = jQuery(
                            "[name='" + jQuery(this).attr('name') + "-url']").val()
                            + subtype;

                        if (basetype == "json_static") {
                            switch (subtype) {
                                case "iso_langs.json":
                                    ufal.submissions.autocomplete_lang_codes(
                                        jQuery(this), url);
                                    break;
                                default:
                                    console.log("lindat/autocomplete does not support this type: " + type);
                                    return;
                            }
                        } else if (basetype == "solr") {
                            switch (subtype) {
                                case "handle_title_ac":
                                    ufal.submissions.autocomplete_solr_handle_field(
                                        jQuery(this), url);
                                    break;
                                case "author_ac":
                                    ufal.submissions.autocomplete_solr_name_field(
                                        jQuery(this), url);
                                    break;
                                case "local.sponsor_ac":
                                    ufal.submissions.autocomplete_solr_sponsor_component(
                                        jQuery(this), url);
                                    break;
                                default:
                                    ufal.submissions.autocomplete_solr_single_field(
                                        jQuery(this), url);
                            }
                        }
                    } else {
                        console.log("lindat/autocomplete does not support this type: " + type);
                        return;
                    }

                    jQuery(this).attr('autocomplete', 'off');
                });
    },

    convert_language_iso_codes: function () {
        var isoFieldsSelectors = '[name="dc_language_iso_selected"] + .ds-interpreted-field, '; //submission
        var numItems = jQuery(isoFieldsSelectors).length;
        if (numItems > 0) {
            var url = ufal.utils.get_dspace_url()
                + 'static/json/iso_langs.json';
            var isoCodesMap = ufal.utils.swap_map(ufal.utils.ajax_json(url));
            jQuery(isoFieldsSelectors).each(function (i) {
                var value = jQuery(this).html();
                var groups = value.match(/^(\s*)(\w{3})(.*)$/);
                if (groups) {
                    var isoCode = groups[2];
                    if (isoCode in isoCodesMap) {
                        value = groups[1] + isoCodesMap[isoCode] + groups[3];
                    }
                }
                jQuery(this).html(value);
            });
        }
    },

    convert_codes: function () {
        ufal.submissions.convert_language_iso_codes();
    }

};

jQuery(document).ready(function () {
    ufal.submissions.init_openaire();
    ufal.submissions.fix_l10n();
    ufal.submissions.handle_submission_js();
    ufal.submissions.autocomplete();
    ufal.submissions.convert_codes();

    if (jQuery("#aspect_submission_StepTransformer_field_license").length) {
        jQuery("#aspect_submission_StepTransformer_field_license").select2({
            placeholder: {id: "", text: jQuery("#aspect_submission_StepTransformer_field_license option:selected").text()},
            allowClear: true,
            formatResult: function (license) {
                var id = license.id;
                if (id == "") return "<div class='bold' style='padding: 5px;'>" + license.text + "</div>";
                var a = $("#aspect_submission_StepTransformer_list_license-list li a[name='license_" + id + "']");
                var label = a.attr("license-label");
                var label_text = a.attr("license-label-text");
                return "<div class='bold' style='padding: 5px;'><span class='label label-" + label + "'>" + label + "</span> " + license.text + "</div>";
            },
            formatSelection: function (license) {
                var id = license.id;
                if (id == "") return "<div class='bold' style='padding: 5px;'>" + license.text + "</div>";
                var a = $("#aspect_submission_StepTransformer_list_license-list li a[name='license_" + id + "']");
                var label = a.attr("license-label");
                var label_text = a.attr("license-label-text");
                return "<div class='bold' style='font-size: 120%; padding: 5px;'><span class='label label-" + label + "'>" + label + "</span> " + license.text + "</div>";
            }
        });
    }

    if (jQuery(".licenseselector").length) {

        jQuery("#aspect_submission_StepTransformer_field_license").change(function () {
            jQuery("#aspect_submission_StepTransformer_item_license-not-supported-message").addClass("hidden");
        });

        jQuery(".licenseselector").licenseSelector(
            {
                licenses: {
                    'pcedt2': {
                        name: 'CC-BY-NC-SA + LDC99T42',
                        available: true,
                        url: 'https://lindat.mff.cuni.cz/repository/xmlui/page/license-pcedt2',
                        description: 'License Agreement for Prague Czech English Dependency Treebank 2.0',
                        categories: ['data', 'by', 'nc', 'sa'],
                    },
                    'cnc': {
                        name: 'Czech National Corpus (Shuffled Corpus Data)',
                        available: true,
                        url: 'https://lindat.mff.cuni.cz/repository/xmlui/page/license-cnc',
                        description: 'License Agreement for the CNC',
                        categories: ['data'],
                    },
                    'hamledt': {
                        name: 'HamleDT 1.0 Licence Agreement',
                        available: true,
                        url: 'https://lindat.mff.cuni.cz/repository/xmlui/page/licence-hamledt',
                        description: 'License Agreement for the HamleDT 1.0',
                        categories: ['data'],
                    },
                    'hamledt-2.0': {
                        name: 'HamleDT 2.0 Licence Agreement',
                        available: true,
                        url: 'https://lindat.mff.cuni.cz/repository/xmlui/page/licence-hamledt-2.0',
                        description: 'HamleDT 2.0 License Agreement',
                        categories: ['data'],
                    },
                    'pdt2': {
                        name: 'PDT 2.0 License',
                        available: true,
                        url: 'https://lindat.mff.cuni.cz/repository/xmlui/page/license-pdt2',
                        description: 'Prague Dependency Treebank, version 2.0 License Agreement',
                        categories: ['data'],
                    },
                    'pdtsl': {
                        name: 'PDTSL',
                        available: true,
                        url: 'https://lindat.mff.cuni.cz/repository/xmlui/page/licence-pdtsl',
                        description: 'Research-Usage License Agreement for the PDTSL',
                        categories: ['data'],
                    },
                    'apache-2': {
                        url: 'http://www.apache.org/licenses/LICENSE-2.0',
                    },
                    'perl-artistic-2': {
                        url: 'http://opensource.org/licenses/Artistic-2.0',
                    },
                    'test-1': {
                        url: 'http://www.google.com',
                    }
                },
                onLicenseSelected: function (license) {
                    var selectedLic = license["url"];
                    var allLic = jQuery("#aspect_submission_StepTransformer_list_license-list li a");
                    for (var i = 0; i < allLic.length; i++) {
                        if (allLic[i].href == selectedLic) {
                            var id = allLic[i].name.replace("license_", "");
                            $("#aspect_submission_StepTransformer_field_license").select2("val", id);
                            jQuery('html, body').animate({
                                scrollTop: $("#aspect_submission_StepTransformer_list_submit-ufal-license").offset().top
                            }, 10);
                            jQuery("#aspect_submission_StepTransformer_item_license-not-supported-message").addClass("hidden");
                            return;
                        }
                    }
                    jQuery("#aspect_submission_StepTransformer_item_license-not-supported-message").removeClass("hidden");
                }
            }
        );
    }
}); // ready
