declare var jQuery: JQueryStatic;
declare var $: JQueryStatic;

var alloCineIcon = "data:image/png;base64,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";
var rottenTomatoesIcon = "data:image/png;base64,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";

function insertAll(film: any, audioFlags: string, subTitlesFlags: string, languageFlags: string, countryFlags: string, seasons: string) {
    if (seasons !== '') {
        seasons = `, <b>Seasons:</b>&nbsp;${seasons}`
    }
    var name = film.mainTitle === null ? (film.name === null ? '' : film.name) : film.mainTitle;
    var year = film.mainYear === null ? '' : film.mainYear;
    var alloCine = `https://www.allocine.fr/rechercher/?q=${(name + ' ' + year).replaceAll(" ", "+")}`;
    var rottenTomatoes = `https://www.rottentomatoes.com/search?search=${encodeURIComponent(name + ' ' + year)}`;

    var str: string = `
        <div class="hist-outer-cell" align="center">
                <a href="https://www.imdb.com/title/tt${film.mainImdbid}" target ="_blank" style="display: flex; align-items: center; justify-content: center; height: 540px;">
                        <img src="${film.mainCoverUrl}" alt="${film.mainOriginalTitle}" width="300" title="${film.plotPlot}"/>
                </a>
                <div class="hist-text-cell">
                        <span style="font-weight: bold; border:0px; text-wrap: balance;">
                            <a class="ui-button ui-widget ui-corner-all" href="https://www.imdb.com/title/tt${film.mainImdbid}" target ="_blank" title="${film.file}">${film.mainTitle}</a>
                        </span>
                        <span style="font-weight: lighter; font-size: x-small; font-family: monospace; padding: 1em; display: flex; justify-content: center; align-items: center;">
                            <a href="${alloCine}" target="_blank" rel="noreferrer noopener external"><img src="${alloCineIcon}" alt="AlloCiné ${film.mainOriginalTitle}" width="20px" title="AlloCiné"/></a>&nbsp;
                            <a href="${rottenTomatoes}" target="_blank" rel="noreferrer noopener external"><img src="${rottenTomatoesIcon}" alt="Rotten Tomatoes ${film.mainOriginalTitle}" width="20px" title="Rotten Tomatoes"/></a>&nbsp;
                            ${film.mainImdbid}
                         </span>
                        <span style="text-wrap: balance;">${film.mainKind}&nbsp;
                        <span data-color="${film.mainRating} style="font-weight: bold">${film.mainRating}</span>&nbsp;${film.mainVotes}&nbsp;
                        <span style="text-wrap: balance;"><b>${film.runtimeHM}</b>, <i>${film.mainGenres}</i></span>
                        </br>
                        <span style="font-size: small">`;
    if (film.mainAwardsWins > 0 || film.mainAwardsPrestigious_award) {
        str +=
            `<span style="font-weight: bold; color: #104e8b;font-size: medium;">${film.mainAwardsWins > 0 ? "Wins:&nbsp;" + film.mainAwardsWins : ""}${film.mainAwardsPrestigious_award ? ",&nbsp;" + film.mainAwardsPrestigious_award.name : ""}</span>
            </br>`;
    }
    str += `<span style="font-size: medium;"><span>${film.size} ${film.fileCount === null ? '' : ', <b>Count: </b>' + film.fileCount}</span>
            <span style="text-wrap: balance;"><b>Year: </b>${film.mainYears === '' ? film.mainYear : film.mainYears}${seasons} </span></span>
            </br>`;
    str += `<span><b>Res.: </b>${film.resolutionDescription === null ? film.width + 'x' + film.heigth : film.resolutionDescription}, <b>Codec: </b>${film.codecDescription}</span></br>`;
    if (film.mainAspectRatio != undefined && film.mainAspectRatio != null && film.mainAspectRatio != '') {
        str += `<span style="text-wrap: balance;" > <b>Ratio: </b>&nbsp;${film.mainAspectRatio}, </span>`;
    }
    if (film.creactors != undefined && film.creactors != null && film.creactors.length != 0) {
        str += `<span><b>Creactors: </b> ${film.creactors} </span>
                </br>`;
    }
    if (film.mainDirectors != undefined && film.mainDirectors != null && film.mainDirectors.length != 0) {
        str += `<span><b>Director: </b> ${film.mainDirectors} </span>
                </br>`;
    }
    if (film.mainWriters != undefined && film.mainWriters != null && film.mainWriters.length != 0) {
        str += `<span><b>Writer: </b>${[...(new Set(film.mainWriters.split(", ")))].join(', ')} </span>
                </br>`;
    }
    if (film.mainCasts != undefined && film.mainCasts != null && film.mainCasts.length != 0) {
        str += `<span><b>Casts: </b> ${film.mainCasts}</span></br>`;
    }
    str += `
                    <span style = "text-wrap: balance;padding: 0px;margin: 0px;" >
                        <b>Countries: </b>&nbsp;${countryFlags}&nbsp;
                        <b>Language: </b>&nbsp;${languageFlags}
                        </br>
                        <b>Audio: </b>&nbsp;${audioFlags}&nbsp;
                        <b>Sub&nbsp;Title: </b>&nbsp;${subTitlesFlags}
                    </span>
                </span>
            </div>
        </div>`;
    return str;
}

function insertBody(film: any, audioFlags: string, subTitlesFlags: string, languageFlags: string, countryFlags: string, seasons: string) {
    if (seasons !== '') {
        seasons = `, Seasons:&nbsp;${seasons}`
    }
    var str: string = `<tr >
				<td align="center" style="text-align: left;">
					<div style="height: 100%; padding: 1em; display: flex; flex-direction: column;" class="ui-accordion-content ui-corner-all ui-helper-reset ui-widget-content ui-accordion-content-active">
						<span style="font-weight: bold; ">
							<a class="ui-button ui-widget ui-corner-all" href="https://www.imdb.com/title/tt${film.mainImdbid}" target ="_blank">
								${film.mainTitle}
							</a>
							&nbsp;&nbsp;<span data-color="${film.mainRating}">${film.mainRating}</span> ${film.mainVotes}&nbsp;<span style="color: slategray;font-size: medium;">${film.mainAwardsWins > 0 ? "Wins: " + film.mainAwardsWins : ""}&nbsp;${film.mainAwardsPrestigious_award ? ", " + film.mainAwardsPrestigious_award.name : ""}</span>&nbsp;<small style="font-weight: lighter; font-size: small; font-family: monospace;">${film.mainImdbid}</small></span>
						<br/>
						<span><b>${film.mainKind}&nbsp;${film.name} | ${film.originalName}</b></span>
						<span><i>${film.mainGenres}</i></span>
						<table>
							<tr>
								<td rowspan="2"><b>Ratio:</b>&nbsp;${film.mainAspectRatio},&nbsp;<b>Year:</b>&nbsp;${film.mainYear}, Seasons:&nbsp;${seasons}, ${countryFlags}</td>
								<td rowspan="2" style="font-size: x-small;padding: 0px;margin: 0px;"><b>Language:</b>&nbsp;${languageFlags}&nbsp;</td>
								<td style="font-size: x-small;padding: 0px;margin: 0px;"><b>Audio:</b>&nbsp;${audioFlags}</td>
							</tr>
							<tr>
								<td style="font-size: x-small; width: 40em;padding: 0px;margin: 0px;"><b>Sub&nbsp;Title:</b>&nbsp;${subTitlesFlags}</td>
							</tr>
						</table>
						<span><b>Duration: </b>${film.runtimeHM}, <b>Resolution: </b>${film.resolutionDescription === null ? film.width + 'x' + film.heigth : film.resolutionDescription}, <b>Codec: </b>${film.codecDescription}, <b>Size: </b>${film.size} ${film.fileCount === null ? '' : ', <b>Count: </b>' + film.fileCount}</span>`;
    if (film.creactors != undefined && film.creactors != null && film.creactors.length != 0) {
        str += `<span><b>Creactors: </b> ${film.creactors}</span>`;
    }
    if (film.mainDirectors != undefined && film.mainDirectors != null && film.mainDirectors.length != 0) {
        str += `<span><b>Director: </b> ${film.mainDirectors}</span>`;
    }
    if (film.mainWriters != undefined && film.mainWriters != null && film.mainWriters.length != 0) {
        str += `<span><b>Writer: </b> ${film.mainWriters}</span>`;
    }
    if (film.mainCasts != undefined && film.mainCasts != null && film.mainCasts.length != 0) {
        str += `<span><b>Casts: </b> ${film.mainCasts}</span>`;
    }
    str += `<br>
                                        <div class="inner-tabs">
                                                <ul>
                                                        <li><a href="#tabs-1">Plot</a></li>
                                                        <li><a href="#tabs-2">Synopsis</a></li>
                                                </ul>
                                                <div id="tabs-1">
                                                        ${film.plotPlot}
                                                </div>
                                                <div id="tabs-2">
                                                        ${film.plotSynopsis}
                                                </div>
                                        </div>
                                </div>
                        </td>
                        <td width="400px" align="center">
                                <a href="https://www.imdb.com/title/tt${film.mainImdbid}" target ="_blank"><img src="${film.mainCoverUrl}" alt="${film.name}" width="300"/></a>
                        </td>
                </tr>`;
    return str
}

var mc = {
    '0-4.9': 'red',
    '5.0-5.9': 'orangered',
    '6.0-7.4': 'blue',
    '7.5-7.9': 'green',
    '8.0-10.0': 'gold'
};

function between(x: number, min: number, max: number) {
    return x >= min && x <= max;
}

function screenshotPreview() {
    var innerW = innerWidth;
    var innerH = innerHeight;
    $("a.screenshot").hover(
        function(e) {
            var x = e.pageX;
            var y = e.pageY;
            var left = (x + 305) >= innerW ? innerW - 305 : x;
            var top = (y + 422) >= innerH ? innerH - 452 : y;
            $("body").append("<p id='screenshot'><img src='" + this.rel + "' alt='url preview'></p>");
            $("#screenshot")
                .css("top", top + "px")
                .css("left", left + "px")
                .fadeIn("fast");
        },
        function() {
            $("#screenshot").remove();
        }
    );
};

function getCountryFlagsByCode(mainCountryCodes: string[]) {
    var flags: string = '';
    if (mainCountryCodes !== null) {
        for (let lang of mainCountryCodes) {
            if (lang && lang !== "und") {
                if (lang === 'cshh') {
                    lang = 'cz'
                }
                if (lang === 'xko') {
                    lang = 'kr'
                }
                if (lang === 'cmn') {
                    lang = 'zh';
                }
                var country = ISO_3166_1_alpha_2[lang.toUpperCase()] === undefined ? lang : ISO_3166_1_alpha_2[lang.toUpperCase()];
                flags += `<img src='https://flagpedia.net/data/flags/h80/${lang}.webp' height='15px' alt='${country}' title='${country}'> `;
            } else {
                flags += `${lang} `;
            }
        }
    }
    return flags;
}

function getLanguageFlagsByCode2(list: string[]) {
    var flags: string = '';
    if (list != null) {
        for (let lang of list) {
            if (lang && lang !== "und") {
                if (lang === 'cmn') {
                    lang = 'zh';
                }
                var map = json_iso_639_1[lang];
                if (map != undefined) {
                    var country = map['name'];
                    flags += `<img src='https://unpkg.com/language-icons/icons/${lang}.svg' height='15px' alt='${country}' title='${country}'> `;
                } else {
                    flags += `${lang} `;
                }
            }
        }
    }
    return flags;
}

function getaAdioSubTitlesFlagsByCode(list: string[]) {
    var flags: string = '';
    if (list != null) {
        for (let element of list) {
            if (element && element !== "und") {
                var map = json_iso_639_2[element];
                if (map != undefined) {
                    var lang = map['639-1'];
                    if (lang === 'cmn') {
                        lang = 'zh';
                    }
                    var country = map['en'][0];
                    flags += `<img src='https://unpkg.com/language-icons/icons/${lang}.svg' height='15px' alt='${country}' title='${country}'> `;
                } else {
                    flags += `${element} `;
                }
            }
        }
    }
    return flags;
}

var dc: number;
var first;
var second;
var th: { addClass: (arg0: any) => void; };

$(document).ready(function() {
    if (!IS_SLIM) {
        const titles = Object.keys(jsonListAll[0])
            .map((key) => `<th>${key}</th>`)
            .join("");

        const rows = jsonListAll.map((obj) =>
            `<tr>${Object.values(obj)
                .map((val) => `<td>${val}</td>`)
                .join("")}</tr>`
        ).join("");

        $("#table_and_search")[0].innerHTML = `
                                                <thead>
                                                        <tr>
                                                                ${titles}
                                                        </tr>
                                                </thead>
                                                <tbody>
                                                        ${rows}
                                                </tbody>`;

        $('#table_and_search').DataTable({
            retrieve: true,
            info: false,
            ordering: true,
            paging: false,
            colReorder: {
                order: [6, 0, 7, 4, 5, 3, 2, 1, 9, 10, 11, 8]
            },
            order: {
                name: 'rank',
                dir: 'asc'
            },
            columnDefs: [
                {
                    target: 2,
                    visible: false,
                    searchable: false
                }, {
                    target: 5,
                    visible: false,
                    searchable: false
                }, {
                    target: 9,
                    visible: false,
                    searchable: false
                }
            ],
            columns: [
                {
                    data: "mainRating",
                    title: "Rating",
                    render: function(data, type, row, meta) {
                        if (type === 'display') {
                            return "<span data-color='" + data + "'>" + data + "</span>";
                        }
                        return data;
                    }
                },
                {
                    data: "runtimeHM",
                    title: "Duration"
                },
                {
                    data: "mainCoverUrl"
                },
                {
                    data: "mainKind",
                    title: "Kind"
                },
                {
                    data: "mainYear",
                    title: "Year"
                },
                {
                    data: "mainVotes",
                    title: "Votes"
                },
                {
                    data: "rank",
                    title: "Rank"
                },
                {
                    data: "mainOriginalTitle",
                    title: "Title",
                    render: function(data, type, row, meta) {
                        if (type === 'display') {
                            return "<a class='screenshot' href='https://www.imdb.com/title/tt" + row.mainImdbid + "/' target='_blank' rel='" + row.mainCoverUrl + "'>" + data + "</a>";
                        }
                        return data;
                    }
                },
                {
                    data: "mainImdbid",
                    title: "IMDB (id)",
                    render: function(data, type, row, meta) {
                        if (type === 'display') {
                            return data + "&nbsp;<small>(" + row.id + ")</small>";
                        }
                        return data;
                    }
                },
                {
                    data: "id"
                },
                {
                    data: "mainCountries",
                    title: "Countries",
                    render: function(data, type, row, meta) {
                        if (type === 'display') {
                            return data.replace(", <b>Country:</b> ", "").replace(", <b>Countries:</b> ", "");
                        }
                        return data;
                    }
                },
                {
                    data: "mainGenres",
                    title: "Genres"
                }
            ]
        });
    }

    screenshotPreview();

    json_iso_639_2['English'] = json_iso_639_2['eng'];

    var textSheet = "";
    for (let film of jsonByDate) {
        var audioFlags: string = getaAdioSubTitlesFlagsByCode(film.audio);
        var subTitlesFlags: string = getaAdioSubTitlesFlagsByCode(film.subTitles);
        var languageFlags: string = getLanguageFlagsByCode2(film.mainLanguageCodes);
        var countryFlags = getCountryFlagsByCode(film.mainCountryCodes);
        var seasons = (film.seasons != undefined && film.seasons != null && film.seasons != 0) ? `${film.seasons}` : '';
        textSheet += insertAll(film, audioFlags, subTitlesFlags, languageFlags, countryFlags, seasons);
    }
    var textByDate = "";
    for (let film of jsonByDate) {
        var audioFlags: string = getaAdioSubTitlesFlagsByCode(film.audio);
        var subTitlesFlags: string = getaAdioSubTitlesFlagsByCode(film.subTitles);
        var languageFlags: string = getLanguageFlagsByCode2(film.mainLanguageCodes);
        var countryFlags = getCountryFlagsByCode(film.mainCountryCodes);
        var seasons = (film.seasons != undefined && film.seasons != null && film.seasons != 0) ? `${film.seasons}` : '';
        textByDate += insertBody(film, audioFlags, subTitlesFlags, languageFlags, countryFlags, seasons);
    }
    var textByRank = "";
    for (let film of jsonByRank) {
        var audioFlags: string = getaAdioSubTitlesFlagsByCode(film.audio);
        var subTitlesFlags: string = getaAdioSubTitlesFlagsByCode(film.subTitles);
        var languageFlags: string = getLanguageFlagsByCode2(film.mainLanguageCodes);
        var countryFlags = getCountryFlagsByCode(film.mainCountryCodes);
        var seasons = (film.seasons != undefined && film.seasons != null && film.seasons != 0) ? `${film.seasons}` : '';
        textByRank += insertBody(film, audioFlags, subTitlesFlags, languageFlags, countryFlags, seasons);
    }
    var textByName = "";
    for (let film of jsonByName) {
        var audioFlags: string = getaAdioSubTitlesFlagsByCode(film.audio);
        var subTitlesFlags: string = getaAdioSubTitlesFlagsByCode(film.subTitles);
        var languageFlags: string = getLanguageFlagsByCode2(film.mainLanguageCodes);
        var countryFlags = getCountryFlagsByCode(film.mainCountryCodes);
        var seasons = (film.seasons != undefined && film.seasons != null && film.seasons != 0) ? `${film.seasons}` : '';
        textByName += insertBody(film, audioFlags, subTitlesFlags, languageFlags, countryFlags, seasons);
    }
    var textByLength = "";
    for (let film of jsonByLength) {
        var audioFlags: string = getaAdioSubTitlesFlagsByCode(film.audio);
        var subTitlesFlags: string = getaAdioSubTitlesFlagsByCode(film.subTitles);
        var languageFlags: string = getLanguageFlagsByCode2(film.mainLanguageCodes);
        var countryFlags = getCountryFlagsByCode(film.mainCountryCodes);
        var seasons = (film.seasons != undefined && film.seasons != null && film.seasons != 0) ? `${film.seasons}` : '';
        textByLength += insertBody(film, audioFlags, subTitlesFlags, languageFlags, countryFlags, seasons);
    }
    $("#List")[0].innerHTML = `${$("#List")[0].innerHTML} \n
								<div id='tabs-sorted'>
									<ul>
                                                                                <li><a href="#tabs-Sheet">By Sheet</a></li>
										<li><a href="#tabs-Date">By Date</a></li>
										<li><a href="#tabs-Rank">By Rank</a></li>
										<li><a href="#tabs-Name">By Name</a></li>
										<li><a href="#tabs-Length">By Length</a></li>
									</ul>
                                                                        <div id='tabs-Sheet'>
										<table>
											<tbody>${textSheet}</tbody>
										</table>
                                                                        </div>
                                                                        <div id='tabs-Date' >
                                                                            <table>
                                                                                <tbody>${textByDate} </tbody>
                                                                            </table>
                                                                        </div>
                                                                        <div id='tabs-Rank' >
                                                                            <table>
                                                                                <tbody>${textByRank} </tbody>
                                                                            </table>
                                                                        </div>
                                                                        <div id='tabs-Name' >
                                                                            <table>
                                                                                <tbody>${textByName} </tbody>
                                                                            </table>
                                                                        </div>
                                                                        <div id='tabs-Length' >
                                                                            <table>
                                                                                <tbody>${textByLength} </tbody>
                                                                            </table>
                                                                        </div>
                                                                    </div>`;

    if (!IS_SLIM) {
        var textListAll = "";
        for (let film of jsonByDate) {
            textListAll += insertAll(film)
        }
        $("#historyData")[0].innerHTML = `${$("#historyData")[0].innerHTML}
			${textListAll}
			</br>
			<div style="left: 50%; transform: translate(-50%, -50%);" class="ui-button ui-widget ui-corner-all" onclick="window.scrollTo({ top: 0, left: 0, behavior: 'smooth' });">&nbsp;&nbsp;Top&nbsp;&nbsp;</div>`;
    }

    $('span').each(function(index: any) {
        th = $(this);
        dc = parseInt($(this).attr('data-color'), 10);
        $.each(mc, function(name: string, value: any) {
            first = parseInt(name.split('-')[0], 10);
            second = parseInt(name.split('-')[1], 10);
            if (between(dc, first, second)) {
                th.addClass(value);
            }
        });
    });

    $(function() {
        $("#tabs").tabs({
            beforeActivate: function(event, ui) {
                if (ui.newPanel[0].id === 'tabs-Table') {
                    $('#table_and_search_wrapper').css('visibility', 'visible');
                } else {
                    $('#table_and_search_wrapper').css('visibility', 'collapse');
                }

                window.scrollTo({
                    top: 0,
                    left: 0,
                    behavior: 'instant'
                });
            }
        });
        $("#tabs-sorted").tabs();
        $(".inner-tabs").tabs();
        $('#table_and_search_wrapper').css('visibility', 'collapse');
        $("#tabs").addClass('visible');
        $("#tabs").removeClass('hidden');
        $("#loader").css('display', 'none');
    });
});
