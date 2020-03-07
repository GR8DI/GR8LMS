<#include "header.ftl">

<div class="row">

    <div class="col-md-12 mt-1">
        <div class="float-right">
            <form class="form-inline" action="/create" method="post">
                <div class="form-group">
                    <input type="text" class="form-control" id="name" name="name" placeholder="New course name">
                </div>
                <button type="submit" class="btn btn-primary">Create</button>
            </form>
        </div>
        <h1 class="display-4">${title}</h1>
    </div>

    <div class="col-md-12 mt-1">
        <#list courses>
            <h2>Courses:</h2>
            <ul>
                <#items as course>
                    <li><a href="/course/${course}">${course}</a></li>
                </#items>
            </ul>
        <#else>
            <p>No course found</p>
        </#list>
    </div>

</div>

<#include "footer.ftl">