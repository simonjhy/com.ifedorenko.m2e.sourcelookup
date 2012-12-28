/*******************************************************************************
 * Copyright (c) 2012 Igor Fedorenko
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Igor Fedorenko - initial API and implementation
 *******************************************************************************/
package com.ifedorenko.m2e.sourcelookup.ui.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.jdt.launching.sourcelookup.containers.PackageFragmentRootSourceContainer;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.m2e.core.embedder.ArtifactKey;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.ifedorenko.m2e.sourcelookup.internal.JDIHelpers;
import com.ifedorenko.m2e.sourcelookup.internal.PomPropertiesScanner;
import com.ifedorenko.m2e.sourcelookup.internal.SourceLookupParticipant;

public class SourceLookupInfoDialog
    extends Dialog
{
    private final Object debugElement;

    private final SourceLookupParticipant sourceLookup;

    private Text textLocation;

    private Text textGAV;

    private Text textJavaProject;

    private Text textSourceContainer;

    // FIXME
    private IProgressMonitor monitor = new NullProgressMonitor();

    public SourceLookupInfoDialog( Shell parentShell, Object debugElement, SourceLookupParticipant sourceLookup )
    {
        super( parentShell );
        setShellStyle( SWT.RESIZE | SWT.TITLE );
        this.debugElement = debugElement;
        this.sourceLookup = sourceLookup;
    }

    @Override
    protected void configureShell( Shell newShell )
    {
        super.configureShell( newShell );
        newShell.setText( "Source lookup properties" );
    }

    @Override
    protected Control createDialogArea( Composite parent )
    {
        Composite container = (Composite) super.createDialogArea( parent );
        container.setLayout( new GridLayout( 2, false ) );

        Label lblCodeLocation = new Label( container, SWT.NONE );
        lblCodeLocation.setLayoutData( new GridData( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblCodeLocation.setText( "Code location:" );

        textLocation = new Text( container, SWT.BORDER | SWT.WRAP | SWT.MULTI );
        textLocation.setEditable( false );
        textLocation.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Label lblGav = new Label( container, SWT.NONE );
        lblGav.setLayoutData( new GridData( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblGav.setText( "GAV:" );

        textGAV = new Text( container, SWT.BORDER | SWT.WRAP );
        textGAV.setEditable( false );
        textGAV.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Label lblJavaProject = new Label( container, SWT.NONE );
        lblJavaProject.setLayoutData( new GridData( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblJavaProject.setText( "Java project:" );

        textJavaProject = new Text( container, SWT.BORDER | SWT.WRAP );
        textJavaProject.setEditable( false );
        textJavaProject.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Label lblSourceContainer = new Label( container, SWT.NONE );
        lblSourceContainer.setLayoutData( new GridData( SWT.RIGHT, SWT.CENTER, false, false, 1, 1 ) );
        lblSourceContainer.setText( "Source container:" );

        textSourceContainer = new Text( container, SWT.BORDER | SWT.WRAP );
        textSourceContainer.setEditable( false );
        textSourceContainer.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false, 1, 1 ) );

        Composite fillerComposite = new Composite( container, SWT.NONE );
        fillerComposite.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, true, true, 2, 1 ) );

        Composite actionsComposite = new Composite( container, SWT.NONE );
        actionsComposite.setLayout( new RowLayout( SWT.HORIZONTAL ) );
        actionsComposite.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, true, false, 2, 1 ) );

        Button btnCopy = new Button( actionsComposite, SWT.NONE );
        btnCopy.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                copyToClipboard();
            }
        } );
        btnCopy.setToolTipText( "Copy to clipboard" );
        btnCopy.setText( "Copy" );

        Button btnRefresh = new Button( actionsComposite, SWT.NONE );
        btnRefresh.addSelectionListener( new SelectionAdapter()
        {
            @Override
            public void widgetSelected( SelectionEvent e )
            {
                try
                {
                    sourceLookup.refreshSourceLookup( debugElement, monitor );
                }
                catch ( CoreException e1 )
                {
                    showError( e1 );
                }
            }
        } );
        btnRefresh.setToolTipText( "Force rediscovery of source lookup information for this code location." );
        btnRefresh.setText( "Refresh" );

        updateDisplay( monitor );

        return container;
    }

    @Override
    protected void createButtonsForButtonBar( Composite parent )
    {

        createButton( parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true );
    }

    @Override
    protected Point getInitialSize()
    {
        return new Point( 450, 300 );
    }

    private void updateDisplay( IProgressMonitor moninot )
    {
        try
        {
            File location = JDIHelpers.getLocation( debugElement );

            if ( location == null )
            {
                return;
            }

            final Collection<ArtifactKey> artifacts = new LinkedHashSet<ArtifactKey>();
            final Collection<IProject> projects = new LinkedHashSet<IProject>();

            new PomPropertiesScanner<Void>()
            {
                @Override
                protected Void visitArtifact( ArtifactKey artifact )
                    throws CoreException
                {
                    artifacts.add( artifact );
                    return null;
                }

                @Override
                protected Void visitMavenProject( IMavenProjectFacade mavenProject )
                {
                    artifacts.add( mavenProject.getArtifactKey() );
                    projects.add( mavenProject.getProject() );
                    return null;
                }

                @Override
                protected Void visitProject( IProject project )
                {
                    projects.add( project );
                    return null;
                }
            }.scan( location );

            textLocation.setText( location.getAbsolutePath() );
            textGAV.setText( artifacts.toString() );
            textJavaProject.setText( projects.toString() );

            ISourceContainer container = sourceLookup.getSourceContainer( debugElement, moninot /* sync */);

            textSourceContainer.setText( toString( container ) );
        }
        catch ( CoreException e )
        {
            showError( e );
        }
    }

    void showError( CoreException e )
    {
        ErrorDialog.openError( getParentShell(), "Source lookup info", "Could not determine code maven coordinates",
                               e.getStatus() );
    }

    private String toString( ISourceContainer container )
    {
        if ( container == null )
        {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append( container.getClass().getSimpleName() ).append( " " ).append( container.getName() );

        if ( container instanceof PackageFragmentRootSourceContainer )
        {
            sb.append( " " ).append( ( (PackageFragmentRootSourceContainer) container ).getPackageFragmentRoot().getJavaProject().getProject() );
        }

        return sb.toString();
    }

    void copyToClipboard()
    {
        List<Transfer> dataTypes = new ArrayList<Transfer>();
        List<Object> data = new ArrayList<Object>();

        Clipboard clipboard = new Clipboard( getShell().getDisplay() );

        StringBuilder sb = new StringBuilder();
        sb.append( "Location: " ).append( textLocation.getText() ).append( "\n" );
        sb.append( "GAV: " ).append( textGAV.getText() ).append( "\n" );
        sb.append( "Java project: " ).append( textJavaProject.getText() ).append( "\n" );
        sb.append( "Source container: " ).append( textSourceContainer.getText() ).append( "\n" );

        dataTypes.add( TextTransfer.getInstance() );
        data.add( sb.toString() );

        clipboard.setContents( data.toArray(), dataTypes.toArray( new Transfer[dataTypes.size()] ) );

        clipboard.dispose();
    }
}